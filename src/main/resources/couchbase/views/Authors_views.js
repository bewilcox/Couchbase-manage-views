
//----------------------------------------------------------------------------------
//-- Find all documents of Author type
//----------------------------------------------------------------------------------
//startview all_Authors
function (doc, meta) {
  if(meta.id.match("Author:")) (
   emit(meta.id, null)
  )
}
//endview

