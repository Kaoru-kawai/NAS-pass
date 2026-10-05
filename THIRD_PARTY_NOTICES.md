# Third-party notices

NAS Pass自身のコードと自作アイコンは、ルートの `LICENSE` にある0BSDライセンスで提供します。依存ライブラリやフォントには、それぞれのライセンスが適用されます。

| Component | License | Source / notice |
| --- | --- | --- |
| Roboto font | SIL Open Font License 1.1 | [同梱ライセンス](android/ROBOTO-LICENSE.txt), [Google Fonts](https://github.com/google/fonts/tree/main/ofl/roboto) |
| Material Components for Android | Apache License 2.0 | [公式リポジトリ](https://github.com/material-components/material-components-android) |
| AndroidX（ExifInterface・AppCompatなど） | Apache License 2.0 | [AndroidX](https://android.googlesource.com/platform/frameworks/support/) |

ライブラリはGradle経由で取得します。再配布時には、実際に同梱する依存関係のライセンス・NOTICEも確認してください。Robotoを含むソースやAPKを配布する際は、そのOFLライセンスを保持してください。APK用のライセンス文は `android/app/src/main/assets/licenses/Roboto-OFL.txt` に同梱しています。

「NAS」の名称に関する権利は各権利者に帰属します。このリポジトリのソフトウェアライセンスは、第三者の名称・商標の権利を付与するものではありません。
