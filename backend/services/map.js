const axios = require('axios');

async function getLocationDetails(locationName) {
  try {
    const response = await axios.get('https://restapi.amap.com/v3/place/text', {
      params: {
        key: process.env.AMAP_API_KEY,
        keywords: locationName,
        offset: 1,
        page: 1,
        extensions: 'all'
      }
    });

    if (response.data.status === '1' && response.data.pois.length > 0) {
      return response.data.pois[0];
    } else {
      return null;
    }
  } catch (error) {
    console.error('Error fetching location details from Amap API:', error);
    return null;
  }
}

module.exports = {
  getLocationDetails
};
