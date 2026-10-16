# InstantUnify Unofficial Version

Unifies duplicate materials. When several mods add their own version of the same material — copper ingots, tin ore,
aluminium dust — modpacks end up with items that look the same but cannot stack or be used interchangeably.
This mod replaces them with one single version, so a pack behaves as if only one mod added that material.

This is an unofficial continuation of [InstantUnify Reforged](https://www.curseforge.com/minecraft/mc-mods/instantunify-reforged)
by GOOGLEPOX, rebuilt and extended.

## Features

| Option | Default | What it does |
|---|---|---|
| `drop` | on | Unifies items when they drop on the ground (block drops, mob loot, items thrown by players). |
| `death` | on | Unifies the drops of dying entities. |
| `change` | on | Unifies inventory slots as soon as they change. |
| `second` | off | Scans and unifies the player inventory once per second. |
| `gui` | off | Unifies the contents of a container when it is opened. Writes into chests and machines, so it is off by default. |
| `harvest` | off | Unifies generated loot: block drops, mob drops, chest loot and fishing. |

Only items **without** custom data take part. Enchanted, damaged or renamed items are always left untouched.

## Configuration

`config/instantunify-common.toml` is the only configuration file.

- **`whitelist` / `blacklist`** — tag names that are unified / ignored, regular expressions allowed.
  The defaults cover the `forge:` material tags and their `c:` counterparts, so the same file works on both
  naming conventions. An entry written as `forge:ores\/.+` also matches the bare tag `forge:ores`, so an item
  that only carries the bare name (silicon is a common example) is recognised too.
- **`listMode`** — how the two lists are combined: `USE_WHITELIST`, `USE_BLACKLIST`, `USE_BOTH_LISTS` (default)
  or `USE_NO_LIST`.
- **`preferredMods`** — the list that decides *which* version wins. The first mod in the list that provides the
  material becomes the dominant item. Mods that are not listed are treated equally. Default order:

  `minecraft, alltheores, allthemodium, kubejs, tconstruct, thermal, thermal_foundation, mekanism, create,
  immersiveengineering, occultism, ae2, chemlib, biggerreactors, silentgear, botania, thermal_extra,
  industrialforegoing, pneumaticcraft, embers, iceandfire, refinedstorage, enderio`

  `minecraft` is first on purpose, so modded copper turns into vanilla copper. Move it to the end if you would
  rather keep the modded version.
- **`blacklistMods`** — items of these mods are never replaced and are never used as a replacement.
- **`exemptMods`** — items of these mods are never replaced, but other mods' items may still turn into them.
  Meant for a mod that owns a material and should stay as it is (`ae2` and its silicon by default).
- **`sameModNoUnify`** — on by default: two items of the same mod are never unified with each other. Mods often
  ship variants that only make sense next to each other.
- **`blacklistItems`** — item ids that are never replaced, for a single variant that has to stay as it is.
  Empty by default.
- **`alternatives`** — tag names that mean the same material, one line per group with the names of a group
  separated by commas: `aluminum,aluminium,bauxite` treats all three as the same material. Note that the default
  also treats bauxite (aluminium ore) as aluminium.
- **`namespaceAliases`** — namespaces that mean the same material, one line per group with the names separated by
  commas. The default `forge,c` treats both as the same, so a mod that ships only one of the two spellings is
  still grouped with the other one.
- **`performance.indexCache` / `performance.lazyIndexRefreshSeconds`** — performance tuning only, they do not
  change the result. Leave them alone unless you are troubleshooting.

Please configure the lists to match your own modpack; the defaults are a starting point, not a complete setup.

### In-game configuration

The configuration can be edited in game from the **Config** button in the mod list, with
[Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) installed. Cloth Config is optional:
without it the mod works exactly the same and the file is edited by hand.

**Settings** holds the switches and the numbers. The lists are kept out of it on purpose: they have a page of
their own, the **List Editor**.

The List Editor works on one list at a time:

- the lists are grouped into tag lists, mod lists, and names and aliases — pick a category, then the list;
- an entry can be edited in place, moved up or down and removed; new entries are added with **Add Entry** (which
  also works while a list is still empty) or with the **+** of a row;
- what the selected list is for is written under the entries, so it never has to be looked up elsewhere;
- **Reset This List** restores the list being edited, **Reset All Lists** restores every list of this mod to the
  defaults documented above;
- **Save** applies what was typed, **Cancel** discards the edits of the list being edited, and switching to
  another list applies them as well.

Changes are applied immediately. On a dedicated server the file of the server is the one that counts, so edit it
there (an operator can run `/instantunify reload` to apply an edit to players that are already online).

### Using it together with Almost Unified

[Almost Unified](https://www.curseforge.com/minecraft/mc-mods/almost-unified) unifies materials at the recipe and
tag level; this mod unifies the items that are already in the world, in inventories and in loot. The two are
complementary and work well together — but they should agree on the same priority order, otherwise a recipe will
produce one mod's ingot and this mod will turn it into another mod's ingot.

Almost Unified ships a mod priority list of its own. Set it to the same order as `preferredMods` above.

The location of that list differs between Almost Unified versions: older versions use a single file
`config/almostunified/unify.json` with a `modPriorities` array, newer versions split the configuration into
several files inside `config/almostunified/` and name the list `mod_priorities`. Open the configuration that your
version actually generated and edit the list there.

## Finding tag names

Press **F3 + H** to enable advanced tooltips, then hover an item: its tag names are listed in the tooltip. Tag
names copied from there can be used directly in `whitelist`, `blacklist` and `alternatives`.

## Notes and limitations

- Items are matched by material: the shape (ore, ingot, gem, ...) plus the material name. The tags are used first;
  when a mod ships no material tag at all the item name decides, so nothing has to be configured per mod. Ores
  keep the stone / deepslate split, a deepslate ore never turns into a stone one.
- Items already stored in chests, machines or storage systems are not touched unless `gui` is enabled.
- Crafting, smelting and machine recipes are not rewritten — that is what Almost Unified is for.
- When two mods have the same priority, the mod that is loaded first wins.
- Because `blacklistMods` also excludes a mod as a replacement target, blacklisting a mod means its items are
  never produced by this mod either.

## License

MIT. Original InstantUnify Reforged by GOOGLEPOX, see the LICENSE file for the full copyright notice.

---

# 瞬间统一 非官方版

统一重复的材料。当多个模组各自添加了同一种材料（铜锭、锡矿、铝粉……）时，整合包里就会出现看起来一样、
却无法堆叠也无法互相替代的物品。本模组把它们替换成同一个版本，让整合包表现得像是只有其中一个模组添加了
这种材料。

本项目是 GOOGLEPOX 的 [InstantUnify Reforged](https://www.curseforge.com/minecraft/mc-mods/instantunify-reforged) 的非官方延续版。

## 功能

| 配置项 | 默认 | 作用 |
|---|---|---|
| `drop` | 开 | 物品掉落到地上时统一（方块掉落、生物掉落、玩家丢弃）。 |
| `death` | 开 | 生物死亡时统一其掉落物。 |
| `change` | 开 | 背包槽位一变就统一。 |
| `second` | 关 | 每秒扫描并统一一次玩家背包。 |
| `gui` | 关 | 打开容器时统一其中的物品。会写入箱子和机器，因此默认关闭。 |
| `harvest` | 关 | 统一生成的战利品：方块掉落、生物掉落、箱子战利品与钓鱼。 |

只有**不带自定义数据**的物品会参与；附魔、带耐久、改过名字的物品一律不动。

## 配置

`config/instantunify-common.toml` 是唯一的配置文件。

- **`whitelist` / `blacklist`** —— 参与统一 / 被忽略的标签名，支持正则表达式。默认值同时覆盖 `forge:` 与
  `c:` 两套材料标签命名，因此同一份配置在两种约定下都能用。写成 `forge:ores\/.+` 的条目**同时匹配裸标签**
  `forge:ores`，所以只打了裸标签的物品（硅是典型例子）也能被识别。
- **`listMode`** —— 两个列表的组合方式：`USE_WHITELIST`、`USE_BLACKLIST`、`USE_BOTH_LISTS`（默认）或
  `USE_NO_LIST`。
- **`preferredMods`** —— 决定**用哪一个版本**的列表。列表中靠前、且提供了该材料的模组成为统一后的物品；
  未列出的模组一视同仁。默认顺序：

  `minecraft, alltheores, allthemodium, kubejs, tconstruct, thermal, thermal_foundation, mekanism, create,
  immersiveengineering, occultism, ae2, chemlib, biggerreactors, silentgear, botania, thermal_extra,
  industrialforegoing, pneumaticcraft, embers, iceandfire, refinedstorage, enderio`

  `minecraft` 放在首位是有意为之：模组铜锭会变成原版铜锭。若你更希望保留模组版本，把它移到最后即可。
- **`blacklistMods`** —— 这些模组的物品既不会被替换，也不会被用作替换目标。
- **`exemptMods`（豁免的模组）** —— 这些模组的物品永远不会被替换，但其他模组的物品可以替换成它们的。
  用于「拥有某材料、且必须保持原样」的模组（默认是 `ae2` 与它的硅）。
- **`sameModNoUnify`（同模组物品不统一）** —— 默认开启：同一个模组内的两个物品不会互相统一。
  模组自带的变体往往只有放在一起才有意义。
- **`blacklistItems`** —— 永远不参与替换的物品 id，用于让某个单独的变体保持原样。默认为空。
- **`alternatives`** —— 表示同一种材料的标签名，一行一组、组内用英文逗号分隔：`aluminum,aluminium,bauxite`
  会把三者视为同一材料。注意默认值同时把铝土矿（bauxite）当作铝。
- **`namespaceAliases`** —— 表示同一种材料的命名空间，一行一组、组内用英文逗号分隔。默认值 `forge,c` 把两者
  视为同一套，因此只写了其中一种拼法的模组也能与另一种归到同一组。
- **`performance.indexCache` / `performance.lazyIndexRefreshSeconds`** —— 纯性能选项，不影响统一结果。
  除排查问题外建议保持默认。

请按你自己的整合包需要配置这些列表；默认值只是起点，不是完整方案。

### 游戏内配置界面

安装 [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) 后，可以在游戏内通过模组列表里的
**配置** 按钮打开配置界面。Cloth Config 是可选的：不装它模组功能完全一样，直接手改配置文件即可。

**设置** 里是各项开关与数值；列表被特意排除在外，它们有单独的页面：**列表编辑器**。

列表编辑器一次只编辑一个列表：

- 列表按用途分为标签列表、模组列表、名称与别名三类，先点分类、再点列表；
- 每一条都可以就地修改、上下移动、删除；新增条目用 **添加条目**（列表为空时同样可用），或用某一行的 **+**；
- 当前列表的用途写在条目下方，不需要另找说明；
- **重置当前列表** 恢复正在编辑的这一列，**重置全部列表** 把本模组的所有列表恢复为上面记录的默认值；
- **保存** 应用已输入的内容，**取消** 放弃当前列表的改动；切换到别的列表时会先把改动应用掉。

修改会立即生效。在专用服务器上以服务端的那份配置为准，请在服务端修改（管理员可用 `/instantunify reload`
把改动立即应用到已经在线的玩家身上）。

### 与 Almost Unified 搭配使用

[Almost Unified](https://www.curseforge.com/minecraft/mc-mods/almost-unified) 在配方与标签层面统一材料，本模组
统一已经存在于世界中、背包里和战利品里的物品，两者互补、可以一起用。但两者的优先级顺序必须一致，否则会
出现「配方产出 A 模组的锭、本模组又把它换成 B 模组的锭」这种情况。

Almost Unified 自带一份模组优先级列表，请把它改成与上面的 `preferredMods` 完全相同的顺序。

该列表的位置随 Almost Unified 版本不同：较旧的版本是单个文件 `config/almostunified/unify.json`，字段名为
`modPriorities`；较新的版本把配置拆成 `config/almostunified/` 下的多个文件，字段名为 `mod_priorities`。
请打开你的版本实际生成的那份配置，在其中修改。

## 查看标签名

按 **F3 + H** 打开高级提示框，然后把鼠标悬停在物品上：提示框里会列出该物品的标签名。从中复制出来的标签名
可以直接填进 `whitelist`、`blacklist` 和 `alternatives`。

## 注意事项与限制

- 物品按「材料」匹配：形状（矿石、锭、宝石……）加材料名。优先取标签；模组完全没打材料标签时退回按物品名判断，
  因此不需要为某个模组单独配置。矿石仍区分石与深板岩，深板岩矿石不会被换成石头矿石。
- 已经存放在箱子、机器或存储系统里的物品不会被处理，除非开启 `gui`。
- 合成、烧炼与机器配方不会被改写，那是 Almost Unified 的职责。
- 当两个模组优先级相同时，先加载的模组胜出。
- `blacklistMods` 同时也会排除替换目标，因此把某个模组列入黑名单，本模组就完全不会产出它的物品。

## 许可证

MIT。原始模组 InstantUnify Reforged 由 GOOGLEPOX 开发，完整版权声明见 LICENSE 文件。
