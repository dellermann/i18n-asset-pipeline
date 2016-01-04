(function (win) {
    var messages = {
/*MESSAGES*/
    };

    win.$L = function (code) {
        var message = messages[code];
        if(message === undefined) {
            return "[" + code + "]";
        } else if(message instanceof Array) {
            var params;
            if (arguments.length === 2 && (arguments[1]) instanceof Array) {
                params = arguments[1];
            } else {
                params = Array.prototype.slice.call(arguments);
                params.shift();
            }
            var result = "";
            for(var i = 0; i < message.length; i++) {
                if(typeof message[i] === "number") {
                    result += params[message[i]];
                } else {
                    result += message[i];
                }
            }
            return result;
        } else {
            return message;
        }
    }
}(this));
