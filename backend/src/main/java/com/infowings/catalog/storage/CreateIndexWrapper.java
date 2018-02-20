package com.infowings.catalog.storage;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Java враппер для вызова метода OClass:
 * OIndex<?> createIndex(String iName, String iType, OProgressListener iProgressListener, ODocument metadata, String algorithm,String... fields);
 * Необходим т.к. Konlin не может отличить этот вызов этого метод от:
 * OIndex<?> createIndex(String iName, INDEX_TYPE iType, OProgressListener iProgressListener, String... fields);
 * по причине наличия в сигнатуре метода параметра с переменным числом аргументов String... fields
 */
final public class CreateIndexWrapper {

    private CreateIndexWrapper() {
    }

    /**
     * @see OClass#createIndex(String iName, String iType, OProgressListener iProgressListener, ODocument metadata, String algorithm,String... fields);
     */
    public static OIndex<?> createIndexWrapper(OClass oClass, String iName, String iType, OProgressListener iProgressListener, ODocument metadata, String algorithm, String[] fields) {
        return oClass.createIndex(iName, iType, iProgressListener, metadata, algorithm, fields);
    }

}
