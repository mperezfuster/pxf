package org.greenplum.pxf.api;

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


/**
 * Thrown when a problem occurs while fetching or parsing a record from the user's input data.
 */
public class BadRecordException extends Exception {
    public BadRecordException() {
    }

    /**
     * Constructs a BadRecordException.
     *
     * @param cause the cause of this exception
     */
    public BadRecordException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a BadRecordException.
     *
     * @param message the cause of this exception
     */
    public BadRecordException(String message) {
        super(message);
    }

    /**
     * Constructs a BadRecordException.
     *
     * @param message the cause of this exception
     * @param cause   the cause of this exception
     */
    public BadRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
