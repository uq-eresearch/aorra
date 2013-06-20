<h3>Group Permissions</h3>

<table class="table">
  <thead>
    <tr>
      <th>Group</th>
      <th>Access Level</th>
    </tr>
  </thead>
  <tbody>
    {#groups}
    <tr>
      <td>{name}</td>
      <td>
        <select name="{name}" data-value="{access}">
          <option value="NONE">No access</option>
          <option value="RO">Read only</option>
          <option value="RW">Read &amp; Write</option>
        </select>
      </td>
    </tr>
    {/groups}
  </tbody>
</table>