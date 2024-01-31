$(function () {
    $(".follow-btn").click(follow);
});

function follow() {
    var btn = this;
    var isFollow;

    if ($(btn).hasClass("btn-info")) {
        isFollow = false;
    } else {
        isFollow = true;
    }

    $.post(CONTEXT_PATH + "/follow/user",
        {"entityType": 'user', "entityId": $(btn).prev().val(), "isFollow": isFollow},
        function (data) {
            data = $.parseJSON(data);
            if (data.code === 0) {
                window.location.reload();
            } else {
                alert(data.msg);
            }
        })
}