const PB = {
  async fetchRSVP() {
    return await pb.collection('rsvp').getFullList({ sort: '-created' });
  },

  async fetchMessages() {
    return await pb.collection('messages').getFullList({ sort: '-created' });
  },

  async update(collection, id, payload) {
    return await pb.collection(collection).update(id, payload);
  },

  async create(collection, payload) {
    return await pb.collection(collection).create(payload);
  }
};