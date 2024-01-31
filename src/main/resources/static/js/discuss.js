$(function(){
    $("#topBtn").click(setTop);
    $("#essenceBtn").click(setEssence);
    $("#deleteBtn").click(setDelete);
});

function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType": entityType, "entityId": entityId, 'entityUserId': entityUserId, 'postId': postId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code === 0) {
                $(btn).children("b").text(data.likeStatus === "1" ? "已赞" : "赞");
                $(btn).children("i").text(entityType === 'post' ? data.likeCount : '(' + data.likeCount + ')');
                location.reload();
            } else {
                alert(data.msg);
            }
        }
    );
}

// 置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code === 0) {
                // $("#topBtn").attr("disabled", "disabled");
                location.reload();
            } else {
                alert(data.msg);
            }
        }
    );
}

// 加精
function setEssence() {
    $.post(
        CONTEXT_PATH + "/discuss/essence",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code === 0) {
                // $("#essenceBtn").attr("disabled", "disabled");
                location.reload();
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code === 0) {
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}