<ul class="breadcrumb">
  {#parents}
  <li>
    <a href="{url}">{name}</a>
    <span class="divider"> / </span>
  </li>
  {/parents}
  {#current}
  <li class="active">
    <span>{name}</span>
  </li>
  {/current}
</ul>