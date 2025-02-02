package com.dorkag.azure_devops.dto.flow

import com.dorkag.azure_devops.dto.LockBehavior
import com.dorkag.azure_devops.dto.Parameter
import com.dorkag.azure_devops.dto.Pool
import com.dorkag.azure_devops.dto.Schedule
import com.dorkag.azure_devops.dto.resources.Resources


data class Pipeline(val name: String? = null,
                    val trigger: List<String>? = null,
                    val pr: List<String>? = null,
                    val pool: Pool,
                    val parameters: List<Parameter>? = null,
                    val variables: Map<String, String>? = null,
                    val stages: List<Stage> = emptyList(),
                    val resources: Resources? = null,
                    val schedules: List<Schedule>? = null,
                    val lockBehavior: LockBehavior? = null,
                    val appendCommitMessageToRunName: Boolean? = null)