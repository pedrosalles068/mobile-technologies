package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

private const val TAG_NOME_SCREEN = "NomeScreen"

data class NomeRankingItem(
    val nome: String,
    val frequencia: Long,
    val ranking: Int
)


private fun formatarFrequencia(frequencia: Long): String {
    return try {
        NumberFormat.getNumberInstance(Locale.GERMANY).format(frequencia)
    } catch (e: Exception) {
        Log.w(TAG_NOME_SCREEN, "Erro ao formatar frequência: '$frequencia'", e)
        frequencia.toString()
    }
}

private suspend fun fetchNomesRanking(localidadeId: String): List<NomeRankingItem> {
    if (localidadeId.isBlank()) {
        Log.w(TAG_NOME_SCREEN, "ID da localidade está vazio, não é possível buscar o ranking de nomes.")
        return emptyList()
    }
    val urlString = "https://servicodados.ibge.gov.br/api/v2/censos/nomes/ranking?localidade=$localidadeId"
    Log.d(TAG_NOME_SCREEN, "Buscando ranking de nomes para localidade: $localidadeId. URL: $urlString")

    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000 
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG_NOME_SCREEN, "Resposta da API de Nomes: $responseBody")
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    
                    val responseObject = jsonArray.getJSONObject(0)
                    val resArray = responseObject.getJSONArray("res")
                    val list = mutableListOf<NomeRankingItem>()
                    for (i in 0 until resArray.length()) {
                        val itemJson = resArray.getJSONObject(i)
                        list.add(
                            NomeRankingItem(
                                nome = itemJson.getString("nome").uppercase(Locale.getDefault()),
                                frequencia = itemJson.getLong("frequencia"),
                                ranking = itemJson.getInt("ranking")
                            )
                        )
                    }
                    Log.d(TAG_NOME_SCREEN, "${list.size} nomes parseados com sucesso.")
                    return@withContext list
                } else {
                    Log.w(TAG_NOME_SCREEN, "Resposta da API de Nomes está vazia ou formato inesperado.")
                }
            } else {
                Log.e(TAG_NOME_SCREEN, "API de Nomes request failed with code: ${connection.responseCode} - ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG_NOME_SCREEN, "Erro ao buscar ou parsear ranking de nomes: ${e.message}", e)
        }
        emptyList()
    }
}

@Composable
fun NomeScreen(
    modifier: Modifier = Modifier, 
    codigoIbge: String?
) {
    var searchText by remember { mutableStateOf("") }
    var rankingList by remember { mutableStateOf<List<NomeRankingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    
    LaunchedEffect(codigoIbge) {
        errorMessage = null
        rankingList = emptyList()
        if (!codigoIbge.isNullOrBlank()) {
            isLoading = true
            Log.d(TAG_NOME_SCREEN, "Iniciando busca de ranking de nomes para IBGE: $codigoIbge")
            try {
                val result = fetchNomesRanking(codigoIbge)
                if (result.isNotEmpty()) {
                    rankingList = result
                } else {
                    errorMessage = "Nenhum ranking de nomes encontrado para esta localidade."
                }
            } catch (e: Exception) {
                Log.e(TAG_NOME_SCREEN, "Exceção no LaunchedEffect ao buscar nomes: ${e.message}", e)
                errorMessage = "Falha ao carregar o ranking de nomes."
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
            errorMessage = "Código IBGE não fornecido. Por favor, obtenha-o na tela \"Cidades\"."
            Log.w(TAG_NOME_SCREEN, "Código IBGE é nulo ou vazio.")
        }
    }

    val filteredList = remember(searchText, rankingList) {
        if (searchText.isBlank()) {
            rankingList
        } else {
            rankingList.filter {
                it.nome.contains(searchText.trim(), ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ranking de Nomes por Localidade",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (codigoIbge.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "Código IBGE não disponível.", 
                color = MaterialTheme.colorScheme.error, 
                textAlign = TextAlign.Center
            )
        } else {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Pesquisar nome") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
                    Text("Carregando ranking...")
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!, 
                        color = MaterialTheme.colorScheme.error, 
                        textAlign = TextAlign.Center
                    )
                }
                rankingList.isEmpty() && !isLoading -> { 
                    Text(
                        "Nenhum dado de ranking disponível para esta localidade ou filtro.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                filteredList.isEmpty() && searchText.isNotBlank() -> {
                     Text(
                        "Nenhum nome encontrado para \"$searchText\".",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredList, key = { it.nome + it.ranking }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.ranking}. ${item.nome}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Freq: ${formatarFrequencia(item.frequencia)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
