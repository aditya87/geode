/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geode.cache.rest.controllers

import org.apache.geode.cache.configuration.RegionAttributesDataPolicy
import org.apache.geode.cache.configuration.RegionAttributesType
import org.apache.geode.cache.configuration.RegionConfig
import org.apache.geode.management.internal.cli.CliUtil
import org.apache.geode.management.internal.cli.functions.CreateRegionFunctionArgs
import org.apache.geode.management.internal.cli.functions.RegionCreateFunction
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

data class RegionCreateParams(val name: String, val type: String)

@RestController
class RegionsController {

    @RequestMapping(method = [RequestMethod.POST], value = ["/regions"],
            produces = ["application/json"], consumes = ["application/json"])
    fun create(@RequestBody params: RegionCreateParams) {
        var regionConfig = RegionConfig()
        regionConfig.name = params.name.split("/").last()

        setAttributes(regionConfig, params.type)

        var functionArgs = CreateRegionFunctionArgs(params.name, regionConfig, true)
//        CliUtil.executeFunction(RegionCreateFunction.INSTANCE, functionArgs,
//                CliUtil.findMembers(["cluster"], null))
    }

    private fun setAttributes(config: RegionConfig, type: String) {
        var attributes = config.regionAttributes
        when(type) {
            "PARTITION" -> {
                attributes.dataPolicy = RegionAttributesDataPolicy.PARTITION
                attributes.partitionAttributes = RegionAttributesType.PartitionAttributes()
            }
            "REPLICATE" -> {
                attributes.dataPolicy = RegionAttributesDataPolicy.REPLICATE
            }
            else -> {}
        }
    }
}