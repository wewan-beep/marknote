import request from '@/utils/request'

export function getNoteList(params) {
  return request.get('/note/list', { params })
}

export function addNote(data) {
  return request.post('/note/add', data)
}

export function updateNote(data) {
  return request.post('/note/update', data)
}

export function deleteNote(data) {
  return request.post('/note/del', data)
}

export function restoreNote(data) {
  return request.post('/note/restore', data)
}

export function removeNote(data) {
  return request.post('/note/remove', data)
}

export function getNote(id) {
  return request.get('/note/get', { params: { id } })
}

export function getHistoryList(noteId) {
  return request.get('/noteHistory/list', { params: { noteId } })
}
