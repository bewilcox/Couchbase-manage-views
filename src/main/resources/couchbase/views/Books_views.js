
//----------------------------------------------------------------------------------
//-- Find all documents with Book type
//----------------------------------------------------------------------------------
//startview all_Books
function (doc, meta) {
  if(meta.id.match("Book:")) (
   emit(meta.id, null)
  )
}
//endview

//----------------------------------------------------------------------------------
//-- Find all documents with Book type by the author id
//----------------------------------------------------------------------------------
//startview Books_by_author
function (doc, meta) {
  if(meta.id.match("Book:")) (
   emit(doc.author, null)
  )
}
//endview

//----------------------------------------------------------------------------------
//-- Find all documents with Book type by the publisher id
//----------------------------------------------------------------------------------
//startview Books_by_publisher
function (doc, meta) {
  if(meta.id.match("Book:")) (
   emit(doc.publisher, null)
  )
}
//endview

