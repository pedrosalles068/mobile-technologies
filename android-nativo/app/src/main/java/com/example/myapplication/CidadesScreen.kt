package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "CidadesScreen"

data class PopLocalData(
    val populacao: String,
    val cidadeNome: String,
    val estadoUF: String
)

private fun formatarPopulacao(populacaoStr: String): String {
    return try {
        val numero = populacaoStr.toLong()
        NumberFormat.getNumberInstance(Locale.GERMANY).format(numero) + " habitantes"
    } catch (e: NumberFormatException) {
        populacaoStr
    }
}

@Composable
private fun ErrorDisplayCard(
    modifier: Modifier = Modifier,
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = "Ícone de Erro",
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Ícone de tentar novamente", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Tentar Novamente")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CidadesScreenContent(
    modifier: Modifier = Modifier,
    onIbgeFetched: (ibgeCode: String) -> Unit
) {
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var locationStatusMessage by remember { mutableStateOf("Verificando permissão...") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(false) }

    var ibge by remember { mutableStateOf("") }
    var dataError by remember { mutableStateOf("") }

    var populacao by remember { mutableStateOf("") }
    var cidadeNomeState by remember { mutableStateOf("") }
    var estadoUFState by remember { mutableStateOf("") }
    var populacaoError by remember { mutableStateOf("") }

    var retryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var locationFetchKey by remember { mutableStateOf(0) }
    var ibgeFetchKey by remember { mutableStateOf(0) }
    var populationFetchKey by remember { mutableStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            
            if (latitude == null && longitude == null) {
                 locationFetchKey++ 
            }
        } else {
            
            locationStatusMessage = "Permissão de localização negada."
            latitude = null
            longitude = null
            
        }
    }

    val fusedLocationClient = remember<FusedLocationProviderClient> {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun resetAllLocalDataStates(notifyIbgeClear: Boolean = true, clearCurrentRetryAction: Boolean = true) {
        val oldIbge = ibge
        ibge = ""
        if (notifyIbgeClear && oldIbge.isNotEmpty()) {
            onIbgeFetched("")
        }
        dataError = ""
        populacao = ""
        cidadeNomeState = ""
        estadoUFState = ""
        populacaoError = ""
        isLoadingData = false
        if(clearCurrentRetryAction) retryAction = null
    }

    LaunchedEffect(hasLocationPermission, locationFetchKey) {
        if (hasLocationPermission) {
            
            if (locationFetchKey > 0) { 
                retryAction = null 
            }
            if (latitude == null && longitude == null) { 
                isLoadingLocation = true
                locationStatusMessage = "Obtendo localização..."
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { loc: Location? ->
                            isLoadingLocation = false
                            if (loc != null) {
                                latitude = loc.latitude
                                longitude = loc.longitude
                                retryAction = null 
                            } else {
                                locationStatusMessage = "Não foi possível obter a localização. Verifique o GPS."
                                resetAllLocalDataStates(clearCurrentRetryAction = false)
                                retryAction = { locationFetchKey++ }
                            }
                        }
                        .addOnFailureListener { e: Exception ->
                            isLoadingLocation = false
                            locationStatusMessage = "Falha ao obter localização: ${e.message ?: "Erro"}"
                            resetAllLocalDataStates(clearCurrentRetryAction = false)
                            retryAction = { locationFetchKey++ }
                        }
                } catch (e: SecurityException) {
                    isLoadingLocation = false
                    locationStatusMessage = "Exceção de segurança: ${e.message ?: "Erro"}"
                    resetAllLocalDataStates(clearCurrentRetryAction = false)
                    retryAction = { locationFetchKey++ }
                }
            }
        } else {
            
            locationStatusMessage = "Permissão de localização necessária."
            latitude = null
            longitude = null
            isLoadingLocation = false
            resetAllLocalDataStates(clearCurrentRetryAction = false) 
            
            retryAction = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        }
    }

    LaunchedEffect(latitude, longitude, ibgeFetchKey) {
        if (latitude != null && longitude != null) {
            if (ibgeFetchKey > 0) retryAction = null
            locationStatusMessage = "Buscando dados da localidade..."
            isLoadingData = true
            resetAllLocalDataStates(notifyIbgeClear = true, clearCurrentRetryAction = false) 
            try {
                val result = getCepAndIbgeFromLatLng(latitude!!, longitude!!)
                val newIbge = result.second ?: ""
                ibge = newIbge
                if (newIbge.isNotEmpty()) {
                    onIbgeFetched(newIbge)
                    retryAction = null 
                } else {
                    dataError = "Não foi possível obter o código IBGE."
                    isLoadingData = false
                    retryAction = { ibgeFetchKey++ }
                }
            } catch (e: Exception) {
                dataError = "Erro ao buscar dados de localidade: ${e.message}"
                isLoadingData = false
                retryAction = { ibgeFetchKey++ }
            }
        } 
    }

    LaunchedEffect(ibge, populationFetchKey) {
        if (ibge.isNotEmpty()) {
            if (dataError.isEmpty()) { 
                if (populationFetchKey > 0) retryAction = null
                locationStatusMessage = "Buscando dados de população..."
                isLoadingData = true 
                populacao = ""
                cidadeNomeState = ""
                estadoUFState = ""
                populacaoError = ""
                try {
                    val popResult = getPopulacaoResidente(ibge)
                    if (popResult != null) {
                        populacao = popResult.populacao
                        cidadeNomeState = popResult.cidadeNome
                        estadoUFState = popResult.estadoUF
                        locationStatusMessage = "Dados da localidade carregados."
                        retryAction = null 
                    } else {
                        populacaoError = "Não foi possível obter os dados de população para este IBGE."
                        retryAction = { populationFetchKey++ }
                    }
                } catch (e: Exception) {
                    populacaoError = "Erro ao buscar dados de população: ${e.message}"
                    retryAction = { populationFetchKey++ }
                } finally {
                    isLoadingData = false
                }
            }
        } 
    }

    val currentErrorMessage = dataError.ifEmpty { populacaoError }
    val showDedicatedErrorDisplay = currentErrorMessage.isNotEmpty() && retryAction != null && !isLoadingLocation && !isLoadingData
    
    val showPermissionRequestUi = !hasLocationPermission && !isLoadingLocation && retryAction != null && !showDedicatedErrorDisplay

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (showPermissionRequestUi) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                Icon(Icons.Filled.LocationSearching, contentDescription = "Ícone de permissão de localização", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Para mostrar dados da sua localidade, precisamos da sua permissão para acessar a localização.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { retryAction?.invoke() }) {
                    Text("Conceder Permissão")
                }
            }
        } else if (showDedicatedErrorDisplay) {
            ErrorDisplayCard(
                errorMessage = currentErrorMessage,
                onRetry = { retryAction?.invoke() }
            )
        } else if (isLoadingLocation || isLoadingData) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Text(
                text = if (isLoadingLocation && !isLoadingData) "Obtendo localização..." 
                       else if (isLoadingData) locationStatusMessage 
                       else locationStatusMessage, 
                style = MaterialTheme.typography.bodySmall, 
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (locationStatusMessage.isNotEmpty() && currentErrorMessage.isEmpty() && !showPermissionRequestUi){
             Text(
                text = locationStatusMessage,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
                color = LocalContentColor.current 
            )
        }
        
        val shouldShowDataCard = !isLoadingLocation && !isLoadingData && !showDedicatedErrorDisplay && !showPermissionRequestUi &&
                                 (cidadeNomeState.isNotEmpty() || (ibge.isNotEmpty() && dataError.isEmpty() && populacaoError.isEmpty()))
        
        if (shouldShowDataCard) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Dados da Localidade",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    if (cidadeNomeState.isNotEmpty()) {
                        InfoRow(icon = Icons.Filled.LocationCity, label = "Cidade:", value = cidadeNomeState, isValueBold = true)
                        if (estadoUFState.isNotEmpty()) {
                            InfoRow(icon = Icons.Filled.Map, label = "Estado:", value = estadoUFState)
                        }
                        if (populacao.isNotEmpty()) {
                            InfoRow(icon = Icons.Filled.People, label = "População:", value = formatarPopulacao(populacao), isValueBold = true)
                        }
                    } else if (ibge.isNotEmpty() && dataError.isEmpty() && populacaoError.isEmpty()) {
                         Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = "Dados não encontrados", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Dados de população não encontrados para esta localidade.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector? = null, label: String, value: String, isValueBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = label, modifier = Modifier.size(20.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(if (icon == null) 100.dp else 80.dp) 
        )
        Text(
            text = value,
            style = if (isValueBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

private suspend fun getCepAndIbgeFromLatLng(lat: Double, lng: Double): Pair<String?, String?> {
    val regexPattern = "\\d{5}-?\\d{3}"
    var cepFromNominatim: String? = null
    var finalCep: String? = null
    var finalIbge: String? = null

    withContext(Dispatchers.IO) {
        try {
            val nominatimUrlString = String.format(
                Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f&addressdetails=1&accept-language=pt-BR",
                lat,
                lng
            )
            val url = URL(nominatimUrlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "CivisPlusApp/1.0") 
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val nominatimJson = JSONObject(responseBody)
                cepFromNominatim = nominatimJson.optJSONObject("address")?.optString("postcode", null)?.trim()
            } else {
                Log.e(TAG, "Nominatim API request failed with code: ${connection.responseCode} - ${connection.responseMessage}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim API call failed", e)
        }

        finalCep = cepFromNominatim

        if (cepFromNominatim != null && cepFromNominatim.matches(Regex(regexPattern))) {
            try {
                val formattedCepForViaCep = cepFromNominatim.replace("-", "")
                val viaCepUrl = URL("https://viacep.com.br/ws/$formattedCepForViaCep/json/")
                val viaCepResponse = viaCepUrl.readText()
                val viaCepJsonObj = JSONObject(viaCepResponse)

                if (!viaCepJsonObj.optBoolean("erro", false)) {
                    finalIbge = viaCepJsonObj.optString("ibge", null)?.trim()
                } else {
                    Log.w(TAG, "ViaCEP returned error for Nominatim CEP: '$cepFromNominatim'.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViaCEP API call failed for Nominatim CEP '$cepFromNominatim'", e)
            }
        } else if (cepFromNominatim != null) {
            Log.w(TAG, "CEP '$cepFromNominatim' from Nominatim is NOT valid by regex. Skipping ViaCEP.")
        } else {
            Log.w(TAG, "No CEP obtained from Nominatim to attempt ViaCEP call.")
        }
    }
    return Pair(finalCep, finalIbge)
}

private suspend fun getPopulacaoResidente(codigoIbge: String): PopLocalData? {
    if (codigoIbge.isBlank()) return null

    val urlString = "https://servicodados.ibge.gov.br/api/v3/agregados/6579/periodos/-1/variaveis/9324?localidades=N6[$codigoIbge]"

    return withContext(Dispatchers.IO) {
        try {
            val jsonResponse = URL(urlString).readText()
            val jsonArray = JSONArray(jsonResponse)

            if (jsonArray.length() > 0) {
                val firstResultObj = jsonArray.getJSONObject(0)
                val resultadosArray = firstResultObj.optJSONArray("resultados")
                if (resultadosArray != null && resultadosArray.length() > 0) {
                    val seriesArray = resultadosArray.getJSONObject(0).optJSONArray("series")
                    if (seriesArray != null && seriesArray.length() > 0) {
                        val firstSeriesObj = seriesArray.getJSONObject(0)
                        val localidadeNomeCompleto = firstSeriesObj.optJSONObject("localidade")?.optString("nome", null)
                        val serieObject = firstSeriesObj.optJSONObject("serie")

                        if (localidadeNomeCompleto != null && serieObject != null) {
                            var populacaoValue: String? = null
                            val keysIterator = serieObject.keys()
                            var maxYear = -1

                            while(keysIterator.hasNext()){
                                val yearKey = keysIterator.next()
                                try {
                                    val yearInt = yearKey.toInt()
                                    if(yearInt > maxYear) {
                                        maxYear = yearInt
                                        populacaoValue = serieObject.getString(yearKey)
                                    }
                                } catch (e: NumberFormatException) {
                                    
                                }
                            }

                            if (populacaoValue != null) {
                                var cidade = localidadeNomeCompleto
                                var uf = ""
                                val regex = Regex("(.*) \\((.*)\\)")
                                val matchResult = regex.find(localidadeNomeCompleto)
                                if (matchResult != null && matchResult.groupValues.size == 3) {
                                    cidade = matchResult.groupValues[1].trim()
                                    uf = matchResult.groupValues[2].trim()
                                } 
                                return@withContext PopLocalData(populacaoValue, cidade, uf)
                            } 
                        } 
                    } 
                } 
            } 
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar ou analisar dados de população: ${e.message}", e)
            null
        }
    }
}
