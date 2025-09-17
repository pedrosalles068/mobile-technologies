# Projeto Civis+ (Tecnologias Móveis)

## 📖 Sobre o Projeto

Este repositório documenta o desenvolvimento do aplicativo **Civis+** em duas plataformas móveis distintas: **Android Nativo (Kotlin/Compose)** e **.NET MAUI (C#/XAML)**. 

O objetivo do projeto foi implementar a mesma aplicação em ambas as tecnologias para analisar, comparar e demonstrar as diferenças e semelhanças entre as abordagens de desenvolvimento nativo e multiplataforma.

## 📱 Versões do Aplicativo

### 1. Android Nativo (`/android-nativo`)

Implementação da aplicação utilizando as ferramentas padrão do ecossistema Android.

* **Tecnologias:**
    * Linguagem: **Kotlin**
    * Interface de Usuário: **Jetpack Compose**
    * IDE: **Android Studio**

### 2. .NET MAUI (`/maui`)

Implementação da mesma aplicação utilizando o framework multiplataforma da Microsoft, .NET MAUI.

* **Tecnologias:**
    * Linguagem: **C#**
    * Interface de Usuário: **XAML**
    * Framework: **.NET MAUI**
    * IDE: **Visual Studio 2022**

## ✨ Funcionalidades Implementadas (em ambas as versões)

- [x] **Navegação por Abas:** Interface principal com seções Home, Cidades e Nomes.
- [x] **Geolocalização:** Obtenção de coordenadas GPS do usuário.
- [x] **Integração com Múltiplas APIs:** Consulta em cadeia às APIs do Nominatim, ViaCEP e IBGE.
- [x] **Exibição de Dados:** Apresentação de dados da localidade (Cidade, Estado, População).
- [x] **Ranking de Nomes:** Exibição de lista rolável com ranking de nomes.
- [x] **Busca/Filtro:** Filtragem em tempo real da lista de nomes.
- [x] **Cache Inteligente:** Armazenamento de dados para carregamento instantâneo.
- [x] **Tratamento de Erros:** Exibição de mensagens amigáveis para falhas de rede.

## 🏁 Conclusão do Projeto

O desenvolvimento paralelo do Civis+ demonstrou com sucesso a capacidade de se atingir paridade funcional e visual utilizando tanto a abordagem nativa quanto a multiplataforma. O estudo permitiu uma análise prática das vantagens, desvantagens e desafios de cada ecossistema, desde a configuração do ambiente até a depuração de problemas específicos de cada plataforma (como o Linker/.NET e o R8/Android).

---
## 👨‍💻 Autores

* Pedro Augusto Gaudencio Salles
* Matheus Selvati Ramos dos Santos
* Miguel Filipe da Silva Cunha
* Luiz Felipe Arcanjo Rangel
