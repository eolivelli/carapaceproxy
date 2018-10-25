/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package nettyhttpproxy.server.cache;

/**
 * Configuration for the ContentsCache
 *
 * @author enrico.olivelli
 */
public class CacheRuntimeConfiguration {

    private final long cacheMaxSize;
    private final long cacheMaxFileSize;

    public CacheRuntimeConfiguration(long cacheMaxSize, long cacheMaxFileSize) {
        this.cacheMaxSize = cacheMaxSize;
        this.cacheMaxFileSize = cacheMaxFileSize;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (int) (this.cacheMaxSize ^ (this.cacheMaxSize >>> 32));
        hash = 79 * hash + (int) (this.cacheMaxFileSize ^ (this.cacheMaxFileSize >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CacheRuntimeConfiguration other = (CacheRuntimeConfiguration) obj;
        if (this.cacheMaxSize != other.cacheMaxSize) {
            return false;
        }
        if (this.cacheMaxFileSize != other.cacheMaxFileSize) {
            return false;
        }
        return true;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public long getCacheMaxFileSize() {
        return cacheMaxFileSize;
    }

}