<div>
  Compare 
  {#version}
  <span>
    <span>
    {timestamp}
    </span>
    &nbsp;-&nbsp;
    {#author}
    <a href="{email}">{name}</a>
    {/author}
  </span>
  {/version}
  &nbsp;to&nbsp;
  <select class="input-xlarge">
  {#otherVersions}
    <option value="{name}">{timestamp} - {#author}{name}{/author}</option>
  {/otherVersions}
  </select>
</div>