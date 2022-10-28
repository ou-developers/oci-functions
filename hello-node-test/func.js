const fdk=require('@fnproject/fdk');

fdk.handle(function(input){
// To pass both tests, remove or comment the line above and uncomment the line below
//fdk.handle(function(input, context){ // adding context parameter

  let name = 'World';
  if (input.name) {
    name = input.name;
  }
  console.log('\nInside Node Hello World function')

  return {'message': 'Hello ' + name}
  // To pass both tests, remove or comment the line above and uncomment the line below
  //return {'message': 'Hello ' + name, "ctx":context} // return the context object
  
})
