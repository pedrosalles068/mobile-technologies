using System.Diagnostics;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Linq;
using Microsoft.Maui.ApplicationModel;

namespace CivisPlus;

public class CidadeCacheData
{
    public string Cidade { get; set; }
    public string Uf { get; set; }
    public string Populacao { get; set; }
    public string CodigoIbge { get; set; }
    public DateTime Timestamp { get; set; }
}

public partial class CidadesPage : ContentPage
{
    private readonly HttpClient _httpClient = new();
    private readonly TimeSpan _cacheDuration = TimeSpan.FromMinutes(15);
    private readonly Stopwatch _pageLoadStopwatch = new();

    public CidadesPage()
    {
        InitializeComponent();
        _httpClient.DefaultRequestHeaders.Add("User-Agent", "CivisPlusApp/1.0");
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        _ = LoadPageDataWithCacheLogic();
    }

    private async Task LoadPageDataWithCacheLogic()
    {
        var cachedDataJson = Preferences.Get("last_city_data", string.Empty);
        var needsApiFetch = true;

        if (!string.IsNullOrEmpty(cachedDataJson))
        {
            try
            {
                var cachedData = JsonSerializer.Deserialize<CidadeCacheData>(cachedDataJson);
                if (!SuccessCard.IsVisible)
                {
                    DefinirEstadoSucesso((cachedData.Cidade, cachedData.Uf, cachedData.Populacao));
                    AppState.CodigoIbgeAtual = cachedData.CodigoIbge;
                }
                if (DateTime.UtcNow - cachedData.Timestamp < _cacheDuration)
                {
                    Debug.WriteLine("CACHE: Dados frescos encontrados. Nova busca na internet não é necessária.");
                    needsApiFetch = false;
                }
                else
                {
                    Debug.WriteLine("CACHE: Dados expirados. Iniciando busca por atualização em segundo plano.");
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"CACHE: Erro ao ler o cache: {ex.Message}");
                Preferences.Clear("last_city_data");
            }
        }

        if (needsApiFetch)
        {
            await CarregarDadosDaInternet();
        }
    }

    private async Task CarregarDadosDaInternet()
    {
        _pageLoadStopwatch.Restart();
        if (!SuccessCard.IsVisible)
        {
            DefinirEstadoCarregando(true, "Verificando permissões...");
        }

        try
        {
            var localizacao = await ObterLocalizacaoAtual();
            if (localizacao == null) throw new Exception("Não foi possível obter sua localização.");

            if (!SuccessCard.IsVisible) DefinirEstadoCarregando(true, "Buscando CEP...");
            var cep = await ObterCepDeCoords(localizacao.Latitude, localizacao.Longitude);
            if (string.IsNullOrEmpty(cep) || cep.Length != 8 || !cep.All(char.IsDigit))
            {
                Debug.WriteLine($"API: CEP inválido obtido da localização: {cep}.");
                if (!SuccessCard.IsVisible) throw new Exception("Não foi possível determinar um CEP válido.");
                return;
            }

            if (!SuccessCard.IsVisible) DefinirEstadoCarregando(true, "Buscando código IBGE...");
            var codigoIbge = await ObterIbgeDeCep(cep);
            if (string.IsNullOrEmpty(codigoIbge)) throw new Exception("Não foi possível obter o código IBGE.");

            if (!SuccessCard.IsVisible) DefinirEstadoCarregando(true, "Buscando dados da cidade...");
            var dadosCidade = await ObterDadosPopulacao(codigoIbge);
            if (dadosCidade == null) throw new Exception("Não foi possível carregar dados da população.");

            var (cidade, uf, populacao) = dadosCidade.Value;
            DefinirEstadoSucesso(dadosCidade.Value);
            AppState.CodigoIbgeAtual = codigoIbge;

            var newCacheData = new CidadeCacheData { Cidade = cidade, Uf = uf, Populacao = populacao, CodigoIbge = codigoIbge, Timestamp = DateTime.UtcNow };
            var newCacheDataJson = JsonSerializer.Serialize(newCacheData);
            Preferences.Set("last_city_data", newCacheDataJson);
            Debug.WriteLine("CACHE: Novos dados salvos com sucesso.");
        }
        catch (Exception ex)
        {
            if (!SuccessCard.IsVisible)
            {
                DefinirEstadoErro(ex.Message);
            }
            Debug.WriteLine($"==> ERRO AO ATUALIZAR DADOS: {ex.Message}");
        }
    }

    private async Task<Location> ObterLocalizacaoAtual()
    {
        var stopwatch = Stopwatch.StartNew();
        var status = await Permissions.CheckStatusAsync<Permissions.LocationWhenInUse>();
        if (status != PermissionStatus.Granted)
        {
            status = await Permissions.RequestAsync<Permissions.LocationWhenInUse>();
            if (status != PermissionStatus.Granted) return null;
        }
        var location = await Geolocation.GetLocationAsync(new GeolocationRequest { DesiredAccuracy = GeolocationAccuracy.Medium, Timeout = TimeSpan.FromSeconds(30) });
        stopwatch.Stop();
        Debug.WriteLine($"PERFORMANCE: Localização obtida em {stopwatch.ElapsedMilliseconds} ms.");
        return location;
    }

    private async Task<string> ObterCepDeCoords(double latitude, double longitude)
    {
        var stopwatch = Stopwatch.StartNew();
        var url = string.Format(System.Globalization.CultureInfo.InvariantCulture, "https://nominatim.openstreetmap.org/reverse?format=json&lat={0:F6}&lon={1:F6}&addressdetails=1&accept-language=pt-BR", latitude, longitude);
        var response = await _httpClient.GetStringAsync(url);
        var json = JsonNode.Parse(response);
        stopwatch.Stop();
        Debug.WriteLine($"PERFORMANCE: API Nominatim (CEP) finalizada em {stopwatch.ElapsedMilliseconds} ms.");
        return json?["address"]?["postcode"]?.ToString().Replace("-", "");
    }

    private async Task<string> ObterIbgeDeCep(string cep)
    {
        var stopwatch = Stopwatch.StartNew();
        var url = $"https://viacep.com.br/ws/{cep}/json/";
        var response = await _httpClient.GetStringAsync(url);
        var json = JsonNode.Parse(response);
        stopwatch.Stop();
        Debug.WriteLine($"PERFORMANCE: API ViaCEP (IBGE) finalizada em {stopwatch.ElapsedMilliseconds} ms.");
        return json?["ibge"]?.ToString();
    }

    private async Task<(string Cidade, string Uf, string Populacao)?> ObterDadosPopulacao(string codigoIbge)
    {
        var stopwatch = Stopwatch.StartNew();
        var url = $"https://servicodados.ibge.gov.br/api/v3/agregados/6579/periodos/-1/variaveis/9324?localidades=N6[{codigoIbge}]";
        var responseStream = await _httpClient.GetStreamAsync(url);
        var jsonArray = JsonDocument.Parse(responseStream).RootElement;
        if (jsonArray.GetArrayLength() == 0) return null;
        var serie = jsonArray[0].GetProperty("resultados")[0].GetProperty("series")[0];
        var localidadeNomeCompleto = serie.GetProperty("localidade").GetProperty("nome").GetString();
        var populacao = serie.GetProperty("serie").EnumerateObject().First().Value.GetString();
        var partesNome = localidadeNomeCompleto.Split('(');
        var cidade = partesNome[0].Trim();
        var uf = partesNome.Length > 1 ? partesNome[1].Replace(")", "").Trim() : "";
        stopwatch.Stop();
        Debug.WriteLine($"PERFORMANCE: API IBGE (População) finalizada em {stopwatch.ElapsedMilliseconds} ms.");
        return (cidade, uf, populacao);
    }

    private async void OnRetryButtonClicked(object sender, EventArgs e)
    {
        await CarregarDadosDaInternet();
    }

    #region UI Control Methods
    private void DefinirEstadoCarregando(bool carregando, string status)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            LoadingLayout.IsVisible = carregando;
            ActivityIndicator.IsRunning = carregando;
            LoadingLabel.Text = status;
            SuccessCard.IsVisible = false;
            ErrorCard.IsVisible = false;
            StatusLabel.IsVisible = false;
        });
    }

    private void DefinirEstadoSucesso((string Cidade, string Uf, string Populacao) dados)
    {
        if (_pageLoadStopwatch.IsRunning)
        {
            _pageLoadStopwatch.Stop();
            Debug.WriteLine($"PERFORMANCE: Tela Cidades exibiu dados da internet em {_pageLoadStopwatch.ElapsedMilliseconds} ms (tempo total).");
        }

        MainThread.BeginInvokeOnMainThread(() =>
        {
            var populacaoFormatada = long.TryParse(dados.Populacao, out var pop) ? $"{pop.ToString("N0", new System.Globalization.CultureInfo("de-DE"))} habitantes" : dados.Populacao;
            CidadeLabel.Text = dados.Cidade;
            EstadoLabel.Text = dados.Uf;
            PopulacaoLabel.Text = populacaoFormatada;
            SuccessCard.IsVisible = true;
            StatusLabel.IsVisible = true;
            StatusLabel.Text = "Dados da localidade carregados.";
            LoadingLayout.IsVisible = false;
            ActivityIndicator.IsRunning = false;
            ErrorCard.IsVisible = false;
        });
    }

    private void DefinirEstadoErro(string mensagem)
    {
        if (_pageLoadStopwatch.IsRunning)
        {
            _pageLoadStopwatch.Stop();
        }

        MainThread.BeginInvokeOnMainThread(() =>
        {
            ErrorLabel.Text = mensagem;
            ErrorCard.IsVisible = true;
            LoadingLayout.IsVisible = false;
            ActivityIndicator.IsRunning = false;
            SuccessCard.IsVisible = false;
            StatusLabel.IsVisible = false;
        });
    }
    #endregion
}