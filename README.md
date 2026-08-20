# Lockview

[English](README.en.md)

[![Test](https://github.com/5ym/lockview-android/actions/workflows/test.yml/badge.svg)](https://github.com/5ym/lockview-android/actions/workflows/test.yml)
[![Release](https://github.com/5ym/lockview-android/actions/workflows/release.yml/badge.svg)](https://github.com/5ym/lockview-android/actions/workflows/release.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

WebView を全画面表示したままロックし、Android 端末をキオスク端末として使うためのアプリです。
ProfileOwner / DeviceOwner を使わずに^1 端末の動作を制限することを目標としています。

^1 両者共にデバイスセットアップ時にアプリの設定が必要になるため既存の端末をそのままキオスク端末化したいため

## How to

1. Android5より搭載されているユーザー機能を使ってこのアプリ用のユーザーを作成する。
2. このアプリをインストールしデフォルトホームとブラウザをこのアプリに設定する。
3. 設定 > ユーザー補助 から「Lockview キオスクモード」を有効にする。(ステータスバーとナビゲーションバーを無効化するために必要)
4. アプリを起動すると設定画面が出るので、表示するURLと解除用パスワードを入力する。
   保存するとそのままキオスクモードが始まる。以降の起動では設定画面は出ない。

## Feature

- [x] webviewの実装
- [x] コールバックでブラウザが開かれる際のサポート(デフォルトブラウザに設定することで対応)
- [x] ホームアプリとして実装(ホームボタン、最近のアプリのボタンを無効化できる)
- [x] 音量ボタン,バックキー無効化(電源ボタンは無効化できなかった物理的な手段で塞ぐべきだと思われる)
- [x] カメラパーミッションの実装(webview内で使用するため)
- [x] 没入モードの実装(上記実装した際には不要かも)
- [x] Lock Taskの実装(上記と同じく)
- [x] 画面常時点灯
- [x] スクリーンショット無効化
- [x] vue等で使うためDOM Storage有効化
- [x] `TYPE_ACCESSIBILITY_OVERLAY`の実装(ステータスバーとナビバーを無効化するため)
- [x] キオスクモード解除ショートカット実装(音量ボタン)
- [x] 起動時のURL・解除用パスワード設定画面と、パスワードによる解除

## 設定と解除

### 初回起動

URLと解除用パスワード(4文字以上)を設定します。URLはスキームを省略すると `https://` を補い、
http/https 以外は受け付けません。設定が終わるまで戻るキーでは抜けられません。

### キオスクモードの解除

音量ボタンを **上→下→上→下→上** の順に5秒以内で押すとパスワード入力ダイアログが出ます。
パスワードが一致すると、ステータスバー/ナビゲーションバーのオーバーレイ、Lock Task、
キー入力の無効化、スクリーンショット禁止、画面常時点灯がすべて解除されます。
ダイアログの「設定を変更」からはURLとパスワードの変更画面へ移動できます(こちらもパスワードが必要)。

解除された状態でもう一度同じ音量シーケンスを入力するとキオスクモードへ復帰します。

シーケンスと制限時間は `MainActivity.UNLOCK_SEQUENCE` と
`UnlockSequenceDetector.DEFAULT_TIMEOUT_MILLIS` で変更できます。

### パスワードを忘れた場合

設定 > アプリ > Lockview からストレージを消去すると未設定状態に戻ります。
パスワードはソルト付きPBKDF2でハッシュ化して保存しているため、復元はできません。

## ステータスバー / ナビゲーションバーの無効化

`SystemBarBlockerService` が `AccessibilityService` として動作し、
`TYPE_ACCESSIBILITY_OVERLAY` のウィンドウをステータスバーとナビゲーションバーの位置に重ねて
タッチを飲み込みます。DeviceOwner を使わずにステータスバーの引き下ろしを塞ぐための実装のため、
ユーザー補助設定から手動で有効にする必要があります。
サービスが無効な場合はアプリ起動時にその旨をトーストで通知します。

なお画面上端 / 下端のシステムバーと同じ高さの領域はオーバーレイがタッチを飲み込むため、
表示するページ側でその位置に操作要素を置かないようにしてください。

## Build

- JDK 17 以上
- Android SDK Platform 37 / Build Tools 36.0.0

```
./gradlew assembleDebug          # ビルド
./gradlew test                   # ローカルユニットテスト
./gradlew connectedAndroidTest   # 実機/エミュレータでのテスト
```

## Release

`v` から始まるタグを push すると GitHub Actions がテストとリリースAPKのビルドを行い、
GitHub Release へ `lockview-<タグ>.apk` を添付します。

```
git tag v1.0.0 && git push origin v1.0.0
```

`versionName` はタグから `v` を除いたもの、`versionCode` はワークフローの実行番号になります。

APK に署名するには、リポジトリの Secrets へ以下を登録してください。
未登録の場合はビルドは通りますが未署名APKになり、端末へインストールできません。

| Secret | 内容 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | キーストアを base64 エンコードしたもの (`base64 -w0 release.jks`) |
| `RELEASE_STORE_PASSWORD` | キーストアのパスワード |
| `RELEASE_KEY_ALIAS` | 鍵のエイリアス |
| `RELEASE_KEY_PASSWORD` | 鍵のパスワード |

## License

AGPL-3.0 と商用ライセンスのデュアルライセンスです。詳細は [LICENSING.md](LICENSING.md) を参照してください。

- **AGPL-3.0** — 無償。再配布やネットワーク越しの提供を行う場合はソースコードの公開義務があります
- **商用ライセンス** — ソースを公開せずに製品へ組み込みたい場合。info@doany.io までお問い合わせください

コントリビューションには[ライセンスに関する同意](CONTRIBUTING.md)が必要です。
