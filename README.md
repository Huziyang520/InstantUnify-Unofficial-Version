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
  naming conventions.
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
- **`alternatives`** — tag names that mean the same material, e.g. `aluminum`, `aluminium` and `bauxite` are all
  treated as the same material. Note that the default also treats bauxite (aluminium ore) as aluminium.
- **`performance.indexCache` / `performance.lazyIndexRefreshSeconds`** — performance tuning only, they do not
  change the result. Leave them alone unless you are troubleshooting.

Please configure the lists to match your own modpack; the defaults are a starting point, not a complete setup.

### In-game configuration

With [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) installed, the configuration can be
edited in game from the **Config** button in the mod list. Cloth Config is optional: without it the mod works
exactly the same and the file is edited by hand.

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
  `c:` 两套材料标签命名，因此同一份配置在两种约定下都能用。
- **`listMode`** —— 两个列表的组合方式：`USE_WHITELIST`、`USE_BLACKLIST`、`USE_BOTH_LISTS`（默认）或
  `USE_NO_LIST`。
- **`preferredMods`** —— 决定**用哪一个版本**的列表。列表中靠前、且提供了该材料的模组成为统一后的物品；
  未列出的模组一视同仁。默认顺序：

  `minecraft, alltheores, allthemodium, kubejs, tconstruct, thermal, thermal_foundation, mekanism, create,
  immersiveengineering, occultism, ae2, chemlib, biggerreactors, silentgear, botania, thermal_extra,
  industrialforegoing, pneumaticcraft, embers, iceandfire, refinedstorage, enderio`

  `minecraft` 放在首位是有意为之：模组铜锭会变成原版铜锭。若你更希望保留模组版本，把它移到最后即可。
- **`blacklistMods`** —— 这些模组的物品既不会被替换，也不会被用作替换目标。
- **`alternatives`** —— 表示同一种材料的标签名，例如 `aluminum`、`aluminium`、`bauxite` 会被视为同一材料。
  注意默认值同时把铝土矿（bauxite）当作铝。
- **`performance.indexCache` / `performance.lazyIndexRefreshSeconds`** —— 纯性能选项，不影响统一结果。
  除排查问题外建议保持默认。

请按你自己的整合包需要配置这些列表；默认值只是起点，不是完整方案。

### 游戏内配置界面

安装 [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) 后，可以在游戏内通过模组列表里的
**配置** 按钮打开配置界面。Cloth Config 是可选的：不装它模组功能完全一样，直接手改配置文件即可。

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

- 已经存放在箱子、机器或存储系统里的物品不会被处理，除非开启 `gui`。
- 合成、烧炼与机器配方不会被改写，那是 Almost Unified 的职责。
- 当两个模组优先级相同时，先加载的模组胜出。
- `blacklistMods` 同时也会排除替换目标，因此把某个模组列入黑名单，本模组就完全不会产出它的物品。

## 许可证

MIT。原始模组 InstantUnify Reforged 由 GOOGLEPOX 开发，完整版权声明见 LICENSE 文件。
