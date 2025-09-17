using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Maui.ApplicationModel;

namespace CivisPlus;

public class RankingList
{
    [JsonPropertyName("res")]
    public List<RankingItem>? Items { get; set; }
}

public class RankingItem
{
    [JsonPropertyName("nome")]
    public string? Nome { get; set; }

    [JsonPropertyName("frequencia")]
    public long Frequencia { get; set; }

    [JsonPropertyName("ranking")]
    public int Rank { get; set; }

    public string FrequenciaFormatada =>
        $"Freq: {Frequencia.ToString("N0", new System.Globalization.CultureInfo("de-DE"))}";
}

public partial class NomePage : ContentPage
{
    private readonly HttpClient _httpClient = new();
    private List<RankingItem> _fullRankingList = new();

    public NomePage()
    {
        InitializeComponent();
        _httpClient.DefaultRequestHeaders.Add("User-Agent", "CivisPlusApp/1.0");
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        
        if (_fullRankingList == null || !_fullRankingList.Any())
        {
            _ = LoadRankingAsync();
        }
    }

    private async Task LoadRankingAsync()
    {
        DefinirEstado(Estado.Carregando);
        try
        {
            var url = "https://servicodados.ibge.gov.br/api/v2/censos/nomes/ranking";
            var jsonResponse = await _httpClient.GetStringAsync(url);

            var rankingData = JsonSerializer.Deserialize<List<RankingList>>(jsonResponse);
            var ranking = rankingData?.FirstOrDefault()?.Items;

            if (ranking != null && ranking.Any())
            {
                DefinirEstado(Estado.Sucesso, ranking);
            }
            else
            {
                throw new Exception("A API retornou uma lista de nomes vazia.");
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"==> ERRO AO BUSCAR RANKING: {ex}");
            
            DefinirEstado(Estado.Erro, errorMessage: ex.Message);
        }
    }

    private void OnRetryButtonClicked(object sender, EventArgs e)
    {
        _ = LoadRankingAsync();
    }

    private void OnNameSearchTextChanged(object sender, TextChangedEventArgs e)
    {
        var textoBusca = e.NewTextValue;
        if (_fullRankingList == null || _fullRankingList.Count == 0)
            return;

        if (string.IsNullOrWhiteSpace(textoBusca))
        {
            NomesCollectionView.ItemsSource = _fullRankingList;
        }
        else
        {
            NomesCollectionView.ItemsSource = _fullRankingList
                .Where(item =>
                    !string.IsNullOrEmpty(item.Nome) &&
                    item.Nome.Contains(textoBusca, StringComparison.OrdinalIgnoreCase))
                .ToList();
        }
    }

    private enum Estado { Carregando, Sucesso, Erro }

    private void DefinirEstado(Estado estado, List<RankingItem>? ranking = null, string? errorMessage = null)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            
            WaitingCard.IsVisible = false;

            LoadingLayout.IsVisible = estado == Estado.Carregando;
            NomesCollectionView.IsVisible = estado == Estado.Sucesso;
            ErrorCard.IsVisible = estado == Estado.Erro;

            if (estado == Estado.Sucesso)
            {
                _fullRankingList = ranking ?? new List<RankingItem>();
                NomesCollectionView.ItemsSource = _fullRankingList;
                NameSearchBar.Text = string.Empty;
            }
            else if (estado == Estado.Erro)
            {
                ErrorLabel.Text = errorMessage ?? "Ocorreu um erro desconhecido.";
            }
        });
    }
}
