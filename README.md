# NAS Pass

スポーツクラブNASの会員証とタオル用QRコードを、すばやく表示するための非公式・個人開発Androidアプリです。

NAS公式の会員証サイトは、利用のたびにログインして会員証を開く必要があり、タオル用の物理QRコードシールを写真に撮って保存・提示する方法も不便でした。そこで、会員証の自動ログイン・表示と、タオル用QRコードの端末内保存・表示を一つのアプリにまとめました。アプリを開けば会員証、左にスワイプすればタオル用QRを表示できます。

## できること

- NASの会員番号・パスワードを端末に保存し、自動ログイン
- ログイン後に公式サイトの会員証QRを自動表示
- 会員証とタオル用QRをタップ・左右スワイプで切り替え
- 写真・スクリーンショットからタオル用QRを登録
- 正方形の切り抜き枠を移動・サイズ調整。90°回転や写真の向き補正にも対応
- タオル画像を端末内に保存し、オフラインでも表示
- Material 3 Expressiveのテーマ・ボタン・ダイアログ・入力欄

Android 8.0（API 26）以降に対応します。会員証を取得するにはインターネット接続が必要です。

## 使い方

1. アプリをインストールして開く。
2. 「設定 → 自動ログインを設定」で、NASの会員番号とパスワードを入力する。
3. 「保存してログイン」を押す。会員証QRが自動で表示される。
4. 「タオル → 画像を選ぶ・変更」で、タオル用QRの画像を選ぶ。
5. 正方形の枠を移動し、四隅やスライダーでサイズを調整する。QRの周囲に白い余白を残す。
6. 「この範囲で保存」で確定する。左スワイプでタオル、右スワイプで会員証へ切り替える。

切り抜きをキャンセルすると、登録済みの画像を保持します。切り抜いた画像はPNGで保存します。

会員証が自動で開かない場合は、NAS画面内の「会員証」をタップしてください。「設定 → 会員証を再読み込み」で再試行できます。NAS側の画面変更・追加認証・メンテナンスによって、自動操作ができなくなる場合があります。

## ビルド環境

| 項目 | バージョン |
| --- | --- |
| JDK | 17以降（確認環境はAndroid Studio付属のJDK 21） |
| Gradle | 8.11.1 |
| Android Gradle Plugin | 8.9.2 |
| Android SDK Platform | 35 |
| Android SDK Build-Tools | 35.0.0 |
| Material Components | 1.14.0 |
| AndroidX ExifInterface | 1.4.1 |

Android StudioのSDK ManagerでSDK Platform 35、Build-Tools 35.0.0、Platform-Toolsを用意してください。SDKライセンスの同意が必要な場合は、SDK Managerで設定します。初回ビルドには依存関係をダウンロードするためのネット接続が必要です。

## WindowsでAPKを作る

PowerShell 7で、リポジトリのルートから実行します。

```powershell
.\android\build.ps1
```

スクリプトの既定値は以下です。

- SDK: `$env:LOCALAPPDATA\Android\Sdk`
- JDK: `C:\Program Files\Android\Android Studio\jbr`

別の場所にインストールしている場合は指定します。

```powershell
.\android\build.ps1 -SdkPath 'D:\Android\Sdk' -JavaPath 'D:\Java\jdk-21'
```

Gradleは公式配布元からダウンロードし、SHA-256を照合してから展開します。Gradle本体・依存関係・テストログは `.tooling/` に置きます。

完成したAPK:

```text
output/NAS-Pass-debug.apk
```

これは開発・個人利用向けのdebug APKです。公開配布用の署名済みAPKやAABは、このスクリプトでは作成しません。署名鍵やパスワードはリポジトリに含めず、配布者の環境で管理してください。

## 実機へインストール

Androidで開発者向けオプションとUSBデバッグを有効にし、PCからの接続を端末で許可します。

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb -s '<端末ID>' install -r .\output\NAS-Pass-debug.apk
& $adb -s '<端末ID>' shell am start -n jp.personal.naspass/.MainActivity
```

`<端末ID>` は `adb devices -l` に表示される自分の端末のIDへ置き換えます。同じ署名のAPKなら `install -r` でアプリデータを保持して更新できます。異なる署名のAPKへ変更するためにアンインストールすると、登録済みの情報・画像も消えます。

## 実機テスト

接続した端末のロックを解除して実行します。

```powershell
.\android\build.ps1 -DeviceTests -Serial '<端末ID>'
```

アプリ本体とテスト専用APKをビルドしてインストールします。テスト用のダミー情報、独立した保存領域、模擬ログイン画面を使用します。ダミー情報をNASへ送信しません。UIテストでアプリ本体を開く際には、既に設定済みの通常の自動ログインが動く場合があります。

検証内容:

- HTTPSの送信先制限と、不正なフォームへの入力拒否
- Android Keystoreでの暗号化・復号・削除
- 特殊文字を含む認証情報の入力と、自動再送信の抑止
- 会員証ボタンの選択、タブ、スワイプ
- 正方形選択の画素対応、画像の端での制限、サイズ変更・復元
- 切り抜き画面の画像読み込み、回転、PNG保存、キャンセル

結果は `.tooling/device-test-results.txt` に保存します。切り抜き画面に未確定の画像がある場合、その画像を保護するため一部のテストをスキップします。

v0.2.0はAndroid 16実機で25項目のテストが成功しています。本人による操作でも自動ログインから会員証QRの表示を確認しました。ジムの入館機・タオルレンタル機での読み取りは、実際の施設で確認してください。

## Gradleを直接使う場合

このリポジトリにはGradle Wrapperを同梱していません。Gradle 8.11.1を用意し、`JAVA_HOME` と `ANDROID_HOME` を設定して実行します。Windows以外でも同じGradleタスクを利用できます。

```sh
gradle -p android assembleDebug
gradle -p android lintDebug
```

APKは `android/app/build/outputs/apk/debug/app-debug.apk` に生成されます。Android Studioで編集する場合は `android/` を開き、Gradleの設定で8.11.1のローカル配布を指定してください。

## データの保存

会員番号・パスワードはAndroid Keystoreの鍵を使い、AES-GCMで暗号化してアプリの内部領域に保存します。ログイン時にはNAS公式HTTPSサイトへ送信します。独自のサーバーへ認証情報や画像をアップロードする機能はありません。

タオル画像も端末内に保存します。会員証は公式ページから表示し、固定画像として保存しません。アプリはバックアップを無効にする設定と、スクリーンショットを防止する設定を使用しています。

登録した情報は設定から削除できます。個人の会員番号・パスワード・QR画像は、IssueやPull Requestにも貼り付けないでください。

## アイコン

青背景（#215BD6）に白文字の「NAS」。自作の文字アイコンであり、公式ロゴの配布ではありません。

- 端末用: アダプティブアイコン。Android 13以降のテーマアイコンにも対応
- Play掲載用: [512×512 PNG](assets/icons/nas-play-icon-512.png)。32ビットRGBA、sRGB、角丸・影の焼き込みなし
- 元データ: [SVG](assets/icons/nas-icon.svg)

[Google Playの公式仕様](https://developer.android.com/distribute/google-play/resources/icon-design-specifications?hl=ja)に従い、Play掲載用とランチャー用を別々に用意しています。

リポジトリのルートで再生成できます。

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' .\tools\GenerateIcon.java (Get-Location).Path
```

生成先は `output/icons/` とAndroidのアイコンリソースです。公開アセットを更新する場合は、生成したPNG・SVGを `assets/icons/` にコピーしてください。

## リポジトリ構成

| パス | 内容 |
| --- | --- |
| `android/` | Androidアプリ、ビルドスクリプト、実機テスト |
| `tools/GenerateIcon.java` | アイコンの生成 |
| `assets/icons/` | 公開用の自作アイコン |
| `output/` | ローカルのAPK・実機画面・検証記録（Git管理対象外） |
| `.tooling/` | Gradleとローカルキャッシュ（Git管理対象外） |

## ライセンス

公開対象の確認結果は [公開前の確認記録](PUBLICATION_CHECK.md) にまとめています。生成物・ログ・端末画面は `.gitignore` で除外しています。

このプロジェクト自身のコードと自作アイコンは [0BSD](LICENSE) です。商用利用・改変・再配布が可能で、著作権表示やライセンス文の転載義務もありません。ソフトウェアは無保証です。

同梱RobotoはSIL Open Font License 1.1、依存するMaterial ComponentsやAndroidXはそれぞれのライセンスが適用されます。[第三者ライセンス](THIRD_PARTY_NOTICES.md)と[Robotoのライセンス](android/ROBOTO-LICENSE.txt)を参照してください。NASの名称・商標に関する権利は各権利者に帰属します。
