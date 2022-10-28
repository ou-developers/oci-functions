// requires func.js registers the function (input, context) with mock fdk
const func = require( './func.js' );
const fdk=require('@fnproject/fdk');

// setting value for 1st parameter
const name ="Your Name"
const input = {"name":name}

//setting value for 2nd parameter
const context = {"_headers":{"Host":"localhost","Content-Type":"application/json"}}

// get the function that was registered in func.js with the (mock) fdk handler
const theFunction = fdk.functionCache() 

// execute 1st test: Expecting "Hello <name>"
test(`Test of func.js for ${name}`, () => {
expect(theFunction(input,context).message)
.toBe(`Hello ${name}`);
});

// execute 2nd test: Expecting the context object
test(`Test of func.js for Host header in context object`, () => {
expect(theFunction(input,context).ctx._headers.Host)
.toBe(`localhost`);
});