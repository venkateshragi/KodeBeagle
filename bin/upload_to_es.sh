#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This script is a helper script to upload spark generated output to elasticSearch.
# DISCLAIMER: There are no defensive checks please use it carefully.

echo "Clearing all indexes from elasticsearch at once."
curl -XDELETE 'http://localhost:9201/_all/'

# create a betterdocs index
curl -XPUT 'http://localhost:9201/betterdocs/'

# Updating mappings and types for betterdocs index.

curl -XPUT 'localhost:9201/betterdocs/custom/_mapping' -d '
{
    "custom" : {
        "properties" : {
                "file" : { "type" : "string", "index" : "no" },
                "tokens": {
                        "type": "nested",
                        "properties": {
                            "importName": {
                                "type": "string",
                                "index" : "not_analyzed"
                            },
                            "lineNumbers": {
                                "type": "long",
                                "index" : "no"
                            }
                        }
                    },
                "score" : { "type" : "integer", "index" : "not_analyzed" }
        }
    }
}'

curl -XPUT 'localhost:9201/repositorysource' -d '{
 "mappings": {
    "typerepositorysource" : {
        "properties" : {
           "repoId" : { "type" : "integer", "index" : "not_analyzed" },
           "sourceFiles": {
                   "type": "nested",
                   "properties": {
                       "fileName": {
                           "type": "string",
                           "index" : "not_analyzed"
                       },
                       "fileContent": {
                           "type": "string",
                           "index" : "no"
                       }
                 }
             }
        }
    }
  }
}'


for f in `find $1 -name 'part*'`
do
    echo "uploading $f to elasticsearch."
    curl -s -XPOST 'localhost:9201/_bulk' --data-binary '@'$f >/dev/null
done
