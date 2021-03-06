package org.greenplum.pxf.service.bridge;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.greenplum.pxf.api.io.Writable;
import org.greenplum.pxf.api.model.Accessor;
import org.greenplum.pxf.api.model.Plugin;
import org.greenplum.pxf.api.model.Resolver;

import java.io.DataInputStream;

/**
 * Bridge interface - defines the interface of the Bridge classes. Any Bridge
 * class acts as an iterator over externally stored data, and should implement
 * getNext (for reading) or setNext (for writing) for handling accessed data.
 */
public interface Bridge {

    /**
     * Starts the iteration for data access.
     *
     * @return true if the operation succeeded
     * @throws Exception when an error occurs during initialization
     */
    boolean beginIteration() throws Exception;

    Writable getNext() throws Exception;

    boolean setNext(DataInputStream inputStream) throws Exception;

    void endIteration() throws Exception;
}
