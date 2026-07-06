# ずんだもん天気予報

Android Auto対応の音声アシスタントアプリ。ずんだもんが現在地の天気予報を読み上げます。

## 機能

- **現在地天気予報**: GPS位置情報に基づいて現在の天気を自動読み上げ
- **ずんだもん音声**: VOICEVOX APIを使用したずんだもんの音声合成
- **リアルタイム天気**: OpenWeatherMap APIで最新の天気情報を取得
- **あいさつ機能**: ずんだもん風のあいさつを再生

## プロジェクト構成

- `MainActivity.java` - スマホ用メインアクティビティ
- `HelloCarAppService.java` - Android Auto連携用Car App Service
- `HelloCarAppSession.java` - カーアプリのセッションハンドラー
- `HelloScreen.java` - Android Auto画面のメインスクリーン

## 動作環境

- Android Studio Hedgehog (2023.1.1) 以降
- Android SDK API Level 34
- 最小SDK: API Level 23
- ターゲットSDK: API Level 34
- 位置情報パーミッションが必要

## セットアップ手順

1. **プロジェクトを開く**
   - Android Studioを起動
   - 「Open an Existing Project」を選択
   - プロジェクトディレクトリに移動

2. **Gradle同期**
   - Android Studioが自動的にGradle同期を促します
   - 「Sync Now」をクリック

3. **プロジェクトのビルド**
   - Build > Make Project
   - または Ctrl+F9 (Windows) / Cmd+F9 (Mac)

4. **デバイスで実行**
   - Android 6.0 (API 23) 以上のデバイスを接続
   - USBデバッグを有効化
   - Run > Run 'app'
   - または Shift+F10 (Windows) / Ctrl+R (Mac)

## Android Autoのテスト

### Desktop Head Unit (DHU)
1. [Android Auto Developer Site](https://developers.google.com/android/auto/desktop-head-unit)からDHUをダウンロード
2. セットアップ手順に従ってスマホをDHUに接続
3. アプリがAndroid Autoランチャーに表示されます

### 実際の車載ディスプレイ
1. Android Auto対応の車にスマホを接続
2. アプリが車載ディスプレイに表示されます

## 主要コンポーネント

### Car App Service
`HelloCarAppService`は`CarAppService`を継承し、Android Autoアプリのエントリーポイントです。アプリ起動時にセッションを作成します。

### Session
`HelloCarAppSession`はアプリセッションを管理し、カーディスプレイ用のスクリーンを作成します。

### Screen
`HelloScreen`は以下の機能を提供します：
- GPS位置情報の取得（FusedLocationProviderClient）
- OpenWeatherMap APIによる天気取得
- VOICEVOX APIによる音声合成
- ListTemplateによるメニュー表示

## 依存関係

- `androidx.car.app:app:1.4.0` - Android Autoコアライブラリ
- `com.squareup.okhttp3:okhttp:4.12.0` - HTTP通信
- `com.google.android.gms:play-services-location:21.1.0` - 位置情報サービス

## APIキー

OpenWeatherMap APIキーがコードに設定されています。本番環境では環境変数や設定ファイルでの管理を推奨します。

## カスタマイズ

アプリをカスタマイズするには：
- `HelloScreen.java`を変更して表示内容を変更
- `ic_launcher_foreground.xml`を変更してアイコンをカスタマイズ
- `strings.xml`でアプリ名を変更
- 位置情報や天気APIのエンドポイントを調整

## TechTalk向けデモポイント

- Android AutoのCar App Library使用
- GPS位置情報取得とAPI連携
- VOICEVOXによる音声合成
- 非同期処理とエラーハンドリング
- 車載環境でのUX設計

## ライセンス

このプロジェクトは教育目的のサンプルプロジェクトです。
