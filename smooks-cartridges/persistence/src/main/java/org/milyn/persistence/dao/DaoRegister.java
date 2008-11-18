/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.persistence.dao;

/**
 * @author maurice_zeijen
 *
 */
public interface DaoRegister<T> {

	/**
	 * Returns the default DAO .
	 * If the repository only has one DAO, like
	 * for instance a facade for the
	 * Session or an EntityManager object
	 *
	 * @param name The name of the DAO
	 * @return The DAO
	 * @throws UnsupportedOperationException if the <tt>getDao()</tt> operation is
     *	          not supported by this DaoRegister.
	 */
	T getDao();

	/**
	 * Returns the DAO using its name to locate it.
	 * If the repository only has one DAO, like
	 * for instance a facade for the
	 * Session or an EntityManager object
	 *
	 * @param name The name of the DAO
	 * @return The DAO
	 * @throws UnsupportedOperationException if the <tt>getDao(String)</tt> operation is
     *	          not supported by this DaoRegister.
	 */
	T getDao(String name);


	/**
	 * Returns the DAO to the registery. This is
	 * usefull if the registery has some
	 * locking or pooling mechanism. If it
	 * doesn't then this class can simply
	 * ignore it.
	 *
	 * @param dao
	 */
	void returnDao(T dao);

}
