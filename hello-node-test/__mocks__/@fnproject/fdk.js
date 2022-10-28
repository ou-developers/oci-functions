const handle = function (f) {
    theFunction = f
    return f
    }
    let theFunction
    const functionCache = function getFunction() {
    return theFunction
    }
    exports.handle = handle
    exports.functionCache = functionCache