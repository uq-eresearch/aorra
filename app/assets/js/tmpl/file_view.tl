<div class="region-breadcrumbs"></div>
<div class="region-buttons pull-right"></div>
<h2>
  <span class="name"
    {#canRename}style="cursor: pointer" title="Click to rename"{/canRename}>
    {name}
  </span>
  <input style="display: none" class="name" value="{name}"/>
</h2>

{#hasWrite}
<h3>Upload new version</h3>
<div class="region-upload"></div>
{/hasWrite}

<div class="region-info"></div>

<br/>
<div class="region-display"></div>