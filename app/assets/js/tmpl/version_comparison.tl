<p>
  Compare
  &nbsp;
  <select class="input-xlarge" style="vertical-align: baseline">
  {#otherVersions}
    <option value="{name}">{timestamp} - {#author}{name}{/author}</option>
  {/otherVersions}
  </select>
  &nbsp;to&nbsp;
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
</p>