# LittleMaidMobResurgence（小女仆：归来）

作者 / Author / 作者：**QIEYYYJ**

[中文](#中文简介) | [English](#english) | [日本語](#日本語)

---

## 中文简介

**小女仆：归来（LittleMaidMobResurgence）** 是基于前辈模组架构二次开发的 Minecraft **1.20.1 Forge** 小女仆模组：

- **1.7.10 LittleMaidMob**（初代小女仆模组）
- **1.12.2 小女改（LittleMaidReengaged）**
- **1.20.1 LittleMaidReBirth 重置版**
- **LittleMaidModelLoader（LMML，小女仆模型加载前置）**

衷心感谢以上所有前辈模组作者们的开创性工作！本模组保留并重构了经典的「契约-召唤-雇佣-工作-战斗-模型/皮肤」体系，
并在此基础上新增了大量拟人化玩法。

### 主要特性

- 契约/雇佣、工资箱、自动发薪与复雇
- 心情值/情绪（愤怒/悲伤/平淡/开心）与好感度等级（Lv1-5，影响最大生命）
- 多种工作模式：农夫、厨师、酿造、熔炉、火把、挖掘、治疗等（按主手物品自动切换）
- 多种战斗模式：剑客、弓手、枪手（TACZ）、三大魔法模组联动
- 自动进食/饥饿系统、糖类互动（回血/回饱食度）、进食动画
- 盾牌格挡（含破盾机制）、反叛系统（代理实体伤害镜像）、避战与休息状态
- 女仆杖（工作范围/女仆绑定器）、女仆捕捉蛋、女仆纪念品（1.12.2 风格复活）
- 饰品栏（Curios）、扩容背包（最多 90 格）、强加载（区块强制加载）
- 敌我识别标签、女仆管理界面、三语（简中/英文/日文）界面与配置

### 兼容联动

- **TACZ（永恒枪械工坊：零）**：枪手模式，自动检索背包/扩容背包弹药换弹
- **Curios**：女仆饰品界面始终可用；仅当安装 Curios 时才有兼容饰品槽位（未安装时仅固定扩容背包道具槽）
- **新生魔艺（Ars Nouveau）**、**Iron's Spells 铁魔法**、**诡厄巫法（Goety）**：魔法战斗模式

### 构建

环境要求：**JDK 17**（Minecraft 1.20.1 Forge 强制）。

```powershell
$env:JAVA_HOME = '<你的 JDK 17 路径>'
.\gradlew.bat :forge:build -x spotlessCheck --console=plain
```

构建产物：`forge\build\libs\Little Maid Mob Resurgence-1.20.1-0.10-Forge.jar`

> **注意**：`forge/build.gradle` 通过 `modCompileOnly files("../libs/*.jar")` 引用部分模组的编译用 jar。
> 首次克隆后需将对应 jar 放入 `libs/` 目录（见 [libs/README.md](libs/README.md)），否则编译期会报依赖缺失。

### 开源协议

本项目以 **MIT 协议**开源（Copyright © 2026 QIEYYYJ）。
基于并致谢前辈小女仆模组系列（LittleMaidMob / LittleMaidReengaged / LittleMaidReBirth / LittleMaidModelLoader），
其内嵌组件保留原许可证；MIT 仅适用于本项目自身代码。详见 [LICENSE](LICENSE)。

---

## English

**LittleMaidMobResurgence** is a Minecraft **1.20.1 Forge** little-maid mod developed on the architecture of the
predecessor maid mods:

- **LittleMaidMob (1.7.10)**
- **LittleMaidReengaged (1.12.2)**
- **LittleMaidReBirth Remake (1.20.1)**
- **LittleMaidModelLoader (LMML)**

Special thanks to all the original mod authors! The classic contract/summon/employ/work/battle/model-skin system is
kept and rebuilt, with many new anthropomorphic features on top.

### Highlights

- Contract/employment, salary boxes, auto payroll and re-contracting
- Mood/emotion (angry/sad/calm/happy) and favorability levels (Lv1-5, affecting max HP)
- Work modes: farming, cooking, brewing, furnace, torches, mining, healing, etc. (auto-switched by held item)
- Battle modes: fencer, archer, gunner (TACZ), and three magic-mod integrations
- Hunger/auto-eating, sugar interaction (heal/satiety), eating animations
- Shield blocking (with shield-break), rebellion system (proxy damage mirroring), evade and rest states
- Maid stick (work-range / maid binder), maid capture egg, maid souvenir (1.12.2-style revival)
- Curios trinket slots, expanded backpack (up to 90 slots), force chunk loading
- Target tagging, maid manager GUI, trilingual UI/config (Simplified Chinese / English / Japanese)

### Compatibilities

- **TACZ**: gunner mode with ammo search across the inventory/expanded backpack
- **Curios**: the maid trinket UI is always available; compatible trinket slots only appear when Curios is installed (otherwise only the fixed backpack-upgrade slot)
- **Ars Nouveau**, **Iron's Spells**, **Goety**: magic battle modes

### Building

Requirements: **JDK 17** (mandatory for MC 1.20.1 Forge).

```powershell
$env:JAVA_HOME = '<your JDK 17 path>'
.\gradlew.bat :forge:build -x spotlessCheck --console=plain
```

Artifact: `forge\build\libs\Little Maid Mob Resurgence-1.20.1-0.10-Forge.jar`

> **Note**: `forge/build.gradle` references some mod jars at compile time via `modCompileOnly files("../libs/*.jar")`.
> After a fresh clone, place the required jars into `libs/` (see [libs/README.md](libs/README.md)), otherwise compilation will fail.

### License

This project is open-sourced under the **MIT License** (Copyright © 2026 QIEYYYJ).
It is based on and thanks to the predecessor little-maid mod series
(LittleMaidMob / LittleMaidReengaged / LittleMaidReBirth / LittleMaidModelLoader), whose bundled
components keep their original licenses; MIT applies only to this project's own code. See [LICENSE](LICENSE).

---

## 日本語

**LittleMaidMobResurgence** は、先人メイドモッド群のアーキテクチャ上に開発した Minecraft **1.20.1 Forge** の
メイドモッドです：

- **LittleMaidMob (1.7.10)**
- **LittleMaidReengaged 小女改 (1.12.2)**
- **LittleMaidReBirth リメイク版 (1.20.1)**
- **LittleMaidModelLoader（LMML）**

先人モッド作者の皆様に心より感謝します。古典的な「契約・召喚・雇用・作業・戦闘・モデル/スキン」体系を
維持・再構築し、その上に多数の擬人化要素を追加しました。

### 主な特徴

- 契約/雇用、給料箱、自動給料・再契約
- 機嫌/感情（怒り・悲しみ・平静・喜び）と好感度レベル（Lv1-5、最大HPに影響）
- 作業モード：農夫・料理・醸造・かまど・松明・採掘・治療など（手持ちアイテムで自動切替）
- 戦闘モード：剣士・弓手・ガンナー（TACZ）・三大魔法モッド連携
- 空腹・自動食事、砂糖での回復（HP/満腹度）、食事アニメーション
- 盾ガード（破盾機構あり）、反乱システム（プロキシ実体へのダメージミラー）、回避・休息状態
- メイドステッキ（作業範囲・メイド登録バインダー）、メイド捕捉卵、メイド記念品（1.12.2風の復活）
- Curios アクセサリ枠、拡張バックパック（最大90スロット）、強制チャンクロード
- 敵味方タグ、メイド管理GUI、三言語UI/コンフィグ（簡体中文/English/日本語）

### 連携

- **TACZ**：ガンナーモード。インベントリ・拡張バックパック内の弾薬を検索してリロード
- **Curios**：メイドのアクセサリUIは常に使用可能。Curios 導入時のみ互換アクセサリスロットが表示されます（未導入時は固定の拡張バックパック枠のみ）
- **Ars Nouveau / Iron's Spells / Goety**：魔法戦闘モード

### ビルド

要件：**JDK 17**（MC 1.20.1 Forge では必須）。

```powershell
$env:JAVA_HOME = '<JDK 17 のパス>'
.\gradlew.bat :forge:build -x spotlessCheck --console=plain
```

成果物：`forge\build\libs\Little Maid Mob Resurgence-1.20.1-0.10-Forge.jar`

> **注意**：`forge/build.gradle` は `modCompileOnly files("../libs/*.jar")` で一部モッドの jar を参照します。
> クローン直後は `libs/` に対応 jar を配置してください（[libs/README.md](libs/README.md) 参照）。無いとコンパイルに失敗します。

### ライセンス

本プロジェクトは **MIT License** で公開されています（Copyright © 2026 QIEYYYJ）。
先人メイドモッド群（LittleMaidMob / LittleMaidReengaged / LittleMaidReBirth / LittleMaidModelLoader）に基づき感謝します。
同梱コンポーネントは各元ライセンスを維持し、MIT は本プロジェクト独自コードにのみ適用されます。詳細は [LICENSE](LICENSE)。
