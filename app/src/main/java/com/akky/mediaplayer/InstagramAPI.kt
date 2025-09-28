package com.akky.mediaplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Task 2: Instagram Metadata Viewer
 * Uses reverse-engineered Instagram GraphQL API
 */
@Composable
fun InstagramAPI() {
    var shortcode by remember { mutableStateOf("DAyZSdyyF67") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Video Metadata Viewer",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = shortcode,
            onValueChange = { shortcode = it },
            label = { Text("Post ID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                if (shortcode.isNotBlank()) {
                    isLoading = true
                    result = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Get Metadata")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (result.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

    }

    LaunchedEffect(isLoading) {
        if (isLoading && shortcode.isNotBlank()) {
            try {
                val metadata = fetchRealInstagramData(shortcode)
                result = metadata
            } catch (e: Exception) {
                result = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

/**
 * Instagram GraphQL API call
 */
private suspend fun fetchRealInstagramData(shortcode: String): String = withContext(Dispatchers.IO) {
    try {
        val url = "https://www.instagram.com/graphql/query"
        
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        val queryPayload = "variables={\"shortcode\":\"$shortcode\"}&doc_id=8845758582119845"
        
        connection.outputStream.use { it.write(queryPayload.toByteArray()) }
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            
            val jsonObject = JSONObject(response)
            
            val data = jsonObject.optJSONObject("data")
            val media = data?.optJSONObject("xdt_shortcode_media")
            
            val caption = media?.optJSONObject("edge_media_to_caption")
                ?.optJSONArray("edges")
                ?.optJSONObject(0)
                ?.optJSONObject("node")
                ?.optString("text") ?: "No caption"
                
            val likeCount = media?.optJSONObject("edge_media_preview_like")
                ?.optInt("count") ?: 0
                
            val commentCount = media?.optJSONObject("edge_media_to_comment")
                ?.optInt("count") ?: 0
                
            val username = media?.optJSONObject("owner")
                ?.optString("username") ?: "Unknown"
            
            """
            Post ID: $shortcode
            Username: @$username
            Caption: ${caption.take(150)}${if (caption.length > 150) "..." else ""}
            Likes: $likeCount
            Comments: $commentCount
            """.trimIndent()
            
        } else {
            "HTTP Error: $responseCode - Instagram API request failed"
        }
        
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
