# libs（编译期依赖 / Compile-time dependencies / コンパイル時依存）

[中文] `forge/build.gradle` 通过 `modCompileOnly files("../libs/*.jar")` 引用以下模组 jar 用于编译。
这些 jar **不会随本仓库分发**（尊重各模组版权），请在克隆后自行从对应模组的发布页下载放入本目录：

[en] `forge/build.gradle` references the following mod jars at compile time via `modCompileOnly files("../libs/*.jar")`.
These jars are **not redistributed** in this repository; download them from the respective mod release pages and place them here:

[ja] `forge/build.gradle` は `modCompileOnly files("../libs/*.jar")` で以下のモッド jar をコンパイル時に参照します。
これらの jar は**本リポジトリでは配布しません**。各モッドのリリースページから入手してこのフォルダに配置してください：

### 需要的 jar / Required jars / 必要な jar

- `tacz-1.20.1.jar` — TACZ 枪械（永恒枪械工坊：零）/ TACZ guns / TACZ 銃
- `curios-forge-5.14.1+1.20.1.jar` — Curios 饰品栏 / Curios trinkets / Curios アクセサリ
- `ars_nouveau-1.20.1-4.1.0.jar` — 新生魔艺 / Ars Nouveau / Ars Nouveau
- `irons_spellbooks-1.20.1-3.16.3.jar` — Iron's Spells 铁魔法 / Iron's Spells / Iron's Spells
- `irons_lib-1.20.1-2.1.0.jar` — Iron's Spells 前置库 / Iron's Spells library / Iron's Spells ライブラリ

[中文] Goety（诡厄巫法）为反射实现，无需编译期 jar。
[en] Goety is implemented via reflection and needs no compile-time jar.
[ja] Goety はリフレクション実装のためコンパイル時 jar は不要です。
