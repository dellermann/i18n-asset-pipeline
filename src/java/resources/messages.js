(function (win) {
    var messages = {
/*MESSAGES*/
    };

    win.$L = function (code) {
        var message = messages[code];
        if(message === undefined) {
            return "[" + code + "]";
        } else {
            return message;
        }
    }
}(this));
