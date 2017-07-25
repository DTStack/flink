/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.execution.librarycache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.CompoundEnumeration;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * 自定义类加载器--->优先从当前加载器获取class
 * Date: 2017/6/18
 * Company: www.dtstack.com
 *
 * @ahthor xuchao
 */

public class DtClassLoader extends URLClassLoader {

	private static Logger log = LoggerFactory.getLogger(DtClassLoader.class);

	private static final String CLASS_FILE_SUFFIX = ".class";

	protected boolean delegate = false;

	/**
	 * The parent class loader.
	 */
	protected ClassLoader parent;

	private boolean hasExternalRepositories = false;

	private Map<String, Class<?>> resourceEntries = new HashMap<>();


	public DtClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.parent = parent;
	}

	public DtClassLoader(URL[] urls) {
		super(urls);
	}

	public DtClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return this.loadClass(name, false);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			if (log.isDebugEnabled()) {
				log.debug("loadClass(" + name + ", " + resolve + ")");
			}
			Class<?> clazz = null;

			// (0) Check our previously loaded local class cache
			clazz = findLoadedClass0(name);
			if (clazz != null) {
				if (log.isDebugEnabled()) {
					log.debug("  Returning class from cache");
				}if (resolve) {
					resolveClass(clazz);
				}
				return (clazz);
			}

			// (0.1) Check our previously loaded class cache
			clazz = findLoadedClass(name);
			if (clazz != null) {
				if (log.isDebugEnabled()) {
					log.debug("  Returning class from cache");
				} if (resolve) {
					resolveClass(clazz);
				}
				return (clazz);
			}


			boolean delegateLoad = delegate || filter(name, true);

			// (1) Delegate to our parent if requested
			if (delegateLoad) {
				if (log.isDebugEnabled()) {
					log.debug("  Delegating to parent classloader1 " + parent);
				}
				try {
					clazz = Class.forName(name, false, parent);
					if (clazz != null) {
						if (log.isDebugEnabled()) {
							log.debug("  Loading class from parent");
						} if (resolve) {
							resolveClass(clazz);
						}
						return (clazz);
					}
				} catch (ClassNotFoundException e) {
					// Ignore
				}
			}

			// (2) Search local repositories
			if (log.isDebugEnabled()) {
				log.debug("  Searching local repositories");
			}
			try {
				clazz = findClass(name);
				if (clazz != null) {
					if (log.isDebugEnabled()) {
						log.debug("  Loading class from local repository");
					} if (resolve) {
						resolveClass(clazz);
					}
					return (clazz);
				}
			} catch (ClassNotFoundException e) {
				// Ignore
			}

			// (3) Delegate to parent unconditionally
			if (!delegateLoad) {
				if (log.isDebugEnabled()) {
					log.debug("  Delegating to parent classloader at end: " + parent);
				}
				try {
					clazz = Class.forName(name, false, parent);
					if (clazz != null) {
						if (log.isDebugEnabled()) {
							log.debug("  Loading class from parent");
						} if (resolve) {
							resolveClass(clazz);
						}
						return (clazz);
					}
				} catch (ClassNotFoundException e) {
					// Ignore
				}
			}
		}

		throw new ClassNotFoundException(name);
	}

	protected Class<?> findLoadedClass0(String name) {

		String path = binaryNameToPath(name, true);

		Class<?> entry = resourceEntries.get(path);
		return entry;
	}

	private String binaryNameToPath(String binaryName, boolean withLeadingSlash) {
		// 1 for leading '/', 6 for ".class"
		StringBuilder path = new StringBuilder(7 + binaryName.length());
		if (withLeadingSlash) {
			path.append('/');
		}
		path.append(binaryName.replace('.', '/'));
		path.append(CLASS_FILE_SUFFIX);
		return path.toString();
	}


	private String nameToPath(String name) {
		if (name.startsWith("/")) {
			return name;
		}
		StringBuilder path = new StringBuilder(
			1 + name.length());
		path.append('/');
		path.append(name);
		return path.toString();
	}


	/**
	 * Returns true if the specified package name is sealed according to the
	 * given manifest.
	 *
	 * @param name Path name to check
	 * @param man  Associated manifest
	 * @return <code>true</code> if the manifest associated says it is sealed
	 */
	protected boolean isPackageSealed(String name, Manifest man) {

		String path = name.replace('.', '/') + '/';
		Attributes attr = man.getAttributes(path);
		String sealed = null;
		if (attr != null) {
			sealed = attr.getValue(Attributes.Name.SEALED);
		}
		if (sealed == null) {
			if ((attr = man.getMainAttributes()) != null) {
				sealed = attr.getValue(Attributes.Name.SEALED);
			}
		}
		return "true".equalsIgnoreCase(sealed);

	}


	@Override
	public URL getResource(String name) {

		if (log.isDebugEnabled()) {
			log.debug("getResource(" + name + ")");
		}

		URL url = null;

		boolean delegateFirst = delegate || filter(name, false);
		//boolean delegateFirst = delegate;

		// (1) Delegate to parent if requested
		if (delegateFirst) {
			if (log.isDebugEnabled()) {
				log.debug("  Delegating to parent classloader " + parent);
			}
			url = parent.getResource(name);
			if (url != null) {
				if (log.isDebugEnabled()) {
					log.debug("  --> Returning '" + url.toString() + "'");
				}
				return (url);
			}
		}

		// (2) Search local repositories
		url = findResource(name);
		if (url != null) {
			if (log.isDebugEnabled()) {
				log.debug("  --> Returning '" + url.toString() + "'");
			}
			return (url);
		}

		// (3) Delegate to parent unconditionally if not already attempted
		if (!delegateFirst) {
			url = parent.getResource(name);
			if (url != null) {
				if (log.isDebugEnabled()) {
					log.debug("  --> Returning '" + url.toString() + "'");
				}
				return (url);
			}
		}

		// (4) Resource was not found
		if (log.isDebugEnabled()) {
			log.debug("  --> Resource not found, returning null");
		}
		return (null);
	}

	@Override
	protected void addURL(URL url) {
		super.addURL(url);
		hasExternalRepositories = true;
	}

	/**
	 * FIXME 需要测试
	 *
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public Enumeration<URL> getResources(String name) throws IOException {
		@SuppressWarnings("unchecked")
		Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[1];
		tmp[0] = findResources(name);//优先使用当前类的资源

		if (!tmp[0].hasMoreElements()) {//只有子classLoader找不到任何资源才会调用原生的方法
			return super.getResources(name);
		}

		return new CompoundEnumeration<>(tmp);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("    findResources(" + name + ")");
		}

		LinkedHashSet<URL> result = new LinkedHashSet<>();

		Enumeration<URL> superResource = super.findResources(name);

		while (superResource.hasMoreElements()) {
			result.add(superResource.nextElement());
		}

		// Adding the results of a call to the superclass
		if (hasExternalRepositories) {
			Enumeration<URL> otherResourcePaths = super.findResources(name);
			while (otherResourcePaths.hasMoreElements()) {
				result.add(otherResourcePaths.nextElement());
			}
		}

		return Collections.enumeration(result);
	}


	protected boolean filter(String name, boolean isClassName) {

		if (name == null) {
			return false;
		}

		if(name.startsWith("org.apache.flink")){

			if(name.startsWith("org.apache.flink.streaming.connectors.kafka")){
				return false;
			}

			if(name.startsWith("org.apache.flink.api.java.io.jdbc")){
				return false;
			}

			if(name.startsWith("org.apache.flink.addons.hbase")){
				return false;
			}

			if(name.startsWith("org.apache.flink.addons.hbase")){
				return false;
			}

			if(name.startsWith("org.apache.flink.streaming.connectors.fs")){
				return false;
			}

			if(name.startsWith("org.apache.flink.streaming.connectors.elasticsearch")){
				return false;
			}

			return true;
		}

		return false;
	}

}
