<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="true" %>

<script>
  let cw_prog = {
    init:function(){
      $('#cw_prog_mock').on('click', ()=> this.mock());
    },
    mock:function(){
      const html = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>진행 상태</h6>
            <p>현재 단계: rinse</p>
            <p>압력: 95 bar</p>
            <p>진행률: 62%</p>
            <p>ETA: 4분</p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;
      $('#cw_prog_result').prepend(html);
    }
  }
  $(()=> cw_prog.init());
</script>

<div class="col-sm-10">
  <h2>세차장 — 진행 상태</h2>
  <div class="row">
    <div class="col-sm-2">
      <button type="button" class="btn btn-secondary" id="cw_prog_mock">샘플 표시</button>
    </div>
  </div>

  <div id="cw_prog_result" class="container p-3 my-3 border" style="overflow:auto;height:300px;"></div>
</div>
