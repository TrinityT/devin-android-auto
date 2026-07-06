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

> **注意:** Android for Cars App Library で作ったテンプレートアプリは、**サイドロード（直接APKインストール）では実車のアプリ一覧に表示されません**。Android Autoの「不明なソース」設定は media / メッセージ / parked アプリ専用で、テンプレートアプリには適用されないためです（[公式ドキュメント](https://developer.android.com/training/cars/testing)）。実車で表示するには、下記のリリースAABを Google Play の **内部アプリ共有** または **内部テスト** で配布し、Playストア経由でインストールしてください。

## リリースビルドとPlay配布

実車で動作確認するには、署名済みのApp Bundle (AAB) を作成し、Google Play経由で配布します。

1. **アップロード鍵（keystore）を作成**
   ```
   keytool -genkeypair -v -keystore upload-keystore.jks \
     -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **`keystore.properties` を用意**
   - `keystore.properties.sample` を `keystore.properties` にコピーし、鍵のパス・パスワード・エイリアスを記入します（`keystore.properties` と `*.jks` はgitignore対象なのでコミットされません）。
3. **AABをビルド**
   ```
   ./gradlew :app:bundleRelease
   ```
   - 生成物: `app/build/outputs/bundle/release/app-release.aab`
4. **Google Playへアップロードして配布**
   - [Play Console](https://play.google.com/console) でアプリを作成し、AABをアップロード
   - **内部アプリ共有** か **内部テスト** トラックで配信リンクを発行（フル審査を通さずに配布可能）
   - スマホでそのリンクを開き、**Playストア経由で**インストール
5. **実車に接続** すると、Android Autoのアプリ一覧に表示されます（事前にAndroid Autoのデベロッパーモードを有効化しておくこと）

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
- MessageTemplateによる天気の読み上げ文言と天気アイコンの単一画面表示

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
