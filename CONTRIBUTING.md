# Contributing

## ライセンスに関する同意 (CLA)

Lockview は AGPL-3.0 と商用ライセンスの[デュアルライセンス](LICENSING.md)で提供しています。
この形態を維持するには、すべてのコードについて単一の権利者が再許諾できる状態を保つ必要があります。

そのため、プルリクエストを送っていただいた時点で、以下に同意したものとみなします。

1. その貢献を提出する権利をあなたが持っていること(第三者の著作物を無断で含んでいないこと)
2. あなたの貢献が AGPL-3.0 で公開されることに同意すること
3. **プロジェクトの権利者(doany)が、あなたの貢献を商用ライセンスを含む他の条件で再許諾する
   ことに同意すること**

3 に同意いただけない場合、その貢献は取り込めません。あらかじめご了承ください。

## 開発

```
./gradlew test                   # ローカルユニットテスト
./gradlew assembleDebug          # ビルド
./gradlew connectedAndroidTest   # 実機/エミュレータでのテスト
```

Android に依存しないロジック(`kiosk` パッケージ内の `PasswordHash`、`UrlValidator`、
`SetupFormValidator`、`UnlockSequenceDetector`、`KioskModeController`)は
ローカルユニットテストで検証しています。ロジックを追加する場合は、可能な限り
Android 非依存の形に切り出してテストを添えてください。

---

# Contributing (English)

## Contributor License Agreement

Lockview is [dual-licensed](LICENSING.md) under AGPL-3.0 and a commercial license.
Keeping that possible requires a single party to be able to relicense all of the code.

By opening a pull request you agree that:

1. You have the right to submit the contribution (it does not include third-party work
   without permission)
2. Your contribution will be published under AGPL-3.0
3. **The project owner (doany) may relicense your contribution under other terms,
   including the commercial license**

Contributions cannot be merged without agreement to point 3.
