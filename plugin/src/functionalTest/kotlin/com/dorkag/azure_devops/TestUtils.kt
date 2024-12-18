package com.dorkag.azure_devops

import java.io.BufferedReader
import java.io.InputStreamReader


/*
* @author: jonathan.gafner
* @created: 19/09/2021
*/
class Anchor

fun getResourceAsText(path: String): String {
    val resourceStream = Anchor::class.java.getResourceAsStream(path)
    val jsonString = resourceStream?.let { BufferedReader(InputStreamReader(it)).readText() }
    return jsonString!!
}




