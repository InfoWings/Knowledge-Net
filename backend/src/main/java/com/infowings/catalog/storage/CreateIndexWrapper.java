package com.infowings.catalog.storage;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
