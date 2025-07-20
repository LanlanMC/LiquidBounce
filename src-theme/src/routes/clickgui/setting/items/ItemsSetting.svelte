<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import {getRegistries} from "../../../../integration/rest";
    import Item from "./Item.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as ItemsSetting;
    const thisPath = `${path}.${cSetting.name}`;

    interface TItem {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let items: TItem[] = [];
    let renderedItems: TItem[] = items;
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    $: {
        let filteredItems = items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

    onMount(async () => {
        let b = (await getRegistries()).items;

        if (b !== undefined) {
            items = b.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleItemToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.identifier];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.identifier);
        }

        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
            <div class="results">
                <VirtualList items={renderedItems} let:item>
                    <Item identifier={item.identifier} name={item.name}
                           enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
                </VirtualList>
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .1s;

    &.expanded {
      margin-bottom: 10px;
    }

    .name {
      color: $clickgui-text-color;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 100px;
    max-height: 500px;
    position: relative;
  }

  .search-input {
    width: 100%;
    border: none;
    border-bottom: solid 1px $accent-color;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: $clickgui-text-color;
    margin-bottom: 5px;
    background-color: rgba($clickgui-base-color, .36);
  }
</style>
