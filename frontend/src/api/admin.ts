import { deleteReq, getJson, postJson, putJson } from './client'

export async function listUsers() {
  return getJson('/api/v1/users')
}

export async function createUser(user: Record<string, unknown>) {
  return postJson('/api/v1/users', user)
}

export async function updateUser(id: number | string, user: Record<string, unknown>) {
  return putJson(`/api/v1/users/${id}`, user)
}

export async function deleteUser(id: number | string) {
  return deleteReq(`/api/v1/users/${id}`)
}

export async function listCardsAdmin() {
  return getJson('/api/v1/cards')
}

export async function createCard(card: Record<string, unknown>) {
  return postJson('/api/v1/cards', card)
}

export async function updateCard(id: string, card: Record<string, unknown>) {
  return putJson(`/api/v1/cards/${id}`, card)
}

export async function deleteCard(id: string) {
  return deleteReq(`/api/v1/cards/${id}`)
}

export async function listRules() {
  return getJson('/api/v1/consume/rules')
}
