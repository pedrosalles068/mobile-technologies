namespace CivisPlus;
using System.Diagnostics;
public partial class MainPage : ContentPage
{
    public MainPage()
    {
        InitializeComponent();
    }
    protected override void OnAppearing()
    {
        base.OnAppearing();

        if (AppState.StartupStopwatch.IsRunning)
        {
            AppState.StartupStopwatch.Stop();
            Debug.WriteLine($"PERFORMANCE: Tela principal (Home) exibida em: {AppState.StartupStopwatch.ElapsedMilliseconds} ms.");
        }
    }
}