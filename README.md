<div align="center">
<p>
    <img width="200" src="https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/liquidbounceLogo.svg" alt="LiquidBounce Logo">
</p>


[Website](https://liquidbounce.net) |
[Forum](https://forums.ccbluex.net) |
[Discord](https://liquidbounce.net/discord) |
[YouTube](https://youtube.com/CCBlueX) |
[X](https://x.com/CCBlueX)
</div>

如果你注意到任何问题或缺少某些功能，你可以通过在[这里](https://github.com/LanlanMC/LiquidBounce/issues)提交Issue。

## 许可

本项目受[GNU 通用公共许可证 v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)的约束。这仅适用于直接位于此存储库中的源代码。在开发和编译过程中，可能会使用我们未获得任何权利的额外源代码。GPL许可证不涵盖此类代码。

对于那些不熟悉许可证的人，以下是其要点的摘要。这不是法律建议，也不具有法律约束力。

*你可以进行的操作：*

- 使用
- 共享
- 修改

*如果您决定使用源代码中的任何代码：*

- **您必须披露您修改后的工作的源代码以及您从该项目中获取的源代码。这意味着不允许在闭源（甚至混淆）应用程序中使用此项目中的代码（即使是部分代码）。**
- **您修改后的应用程序也必须根据 GPL 获得许可**

## 设置工作区

LiquidBounce 使用 Gradle，为确保它安装正确，您可以浏览[Gradle 的网站](https://gradle.org/install/)。对于[主题]([src-theme](https://github.com/LanlanMC/LiquidBounce/tree/nextgen/src-theme))还需要安装 Node.js 和 Python 。

1. 使用 `git clone --recurse-submodules https://github.com/LanlanMC/LiquidBounce` 克隆仓库。

2. CD 到本地repo.

3. 运行`./gradlew genSources`.

4. 在首选 IDE 中将文件夹作为 Gradle 项目打开。

5. 运行客户端

## 其它库

### Mixins

Mixins 可用于在加载类之前在运行时修改类。LiquidBounce 使用它将其代码注入到 Minecraft 客户端。这样，Mojang 任何受版权保护的代码都不会被发送。如果您想了解更多信息，请查看其[文档](https://docs.spongepowered.org/5.1.0/en/plugin/internals/mixins.html)。

## 贡献

我们感谢您的贡献。因此，如果您想支持我们，请随时更改 LiquidBounce 的源代码和提交 Pull Request。

## 统计信息

![Alt](https://repobeats.axiom.co/api/embed/ad3a9161793c4dfe50934cd4442d25dc3ca93128.svg "Repobeats analytics image")
