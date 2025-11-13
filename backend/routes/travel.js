const express = require('express');
const db = require('../database/init');
const { authenticateToken } = require('../middleware/auth');
const { validateTravelPlan } = require('../middleware/validation');
const axios = require('axios');
const { getLocationDetails } = require('../services/map');

const router = express.Router();

// Generate travel plan
router.post('/generate', authenticateToken, validateTravelPlan, async (req, res) => {
  try {
    const {
      destination,
      startDate,
      days,
      travelers,
      budget,
      preferences,
      additionalRequirements
    } = req.body;

    const prompt = `
      You are an expert travel planning API. Your task is to generate a detailed travel plan.
      You MUST respond with ONLY a single, valid JSON object. Do not include any markdown formatting (like \`\`\`json) or any other explanatory text. The JSON object must strictly adhere to the following schema:

      {
        "travel_plan": {
          "destination": "string",
          "start_date": "string (format: YYYY-MM-DD)",
          "duration": "string (e.g., '3天')",
          "travelers": "number",
          "total_budget": "string (e.g., '2000 CNY')",
          "preferences": ["string"],
          "additional_requirements": "string",
          "budget_breakdown": {
            "accommodation": "string",
            "food": "string",
            "transportation": "string",
            "attractions": "string"
          },
          "itinerary": {
            "day_1": {
              "date": "string (format: YYYY-MM-DD)",
              "theme": "string",
              "morning": { "time": "string", "activity": "string", "location": "string", "cost": "string", "description": "string" },
              "lunch": { "time": "string", "activity": "string", "location": "string", "cost": "string", "description": "string" },
              "afternoon": { "time": "string", "activity": "string", "location": "string", "cost": "string", "description": "string" },
              "evening": { "time": "string", "activity": "string", "location": "string", "cost": "string", "description": "string" }
            }
            // ... more days following the same structure
          },
          "accommodation_suggestion": {
            "type": "string",
            "location": "string",
            "estimated_cost": "string",
            "recommendation": "string"
          },
          "transportation_tips": {
            "metro": "string",
            "bus": "string",
            "walking": "string"
          },
          "food_recommendations": ["string"],
          "money_saving_tips": ["string"]
        }
      }

      Now, generate the travel plan for the following user request, strictly adhering to the JSON schema provided above:
      - Destination: ${destination}
      - Start Date: ${startDate}
      - Duration: ${days} days
      - Number of Travelers: ${travelers}
      - Budget: ${budget} CNY
      - Preferences: ${preferences.join(', ')}
      - Additional Requirements: ${additionalRequirements}
    `;

    const response = await axios.post(
      `${process.env.DEEPSEEK_BASE_URL}/v1/chat/completions`,
      {
        model: 'deepseek-chat',
        messages: [
          {
            content: 'You are a helpful travel assistant.',
            role: 'system'
          },
          {
            content: prompt,
            role: 'user'
          }
        ],
        stream: false
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${process.env.DEEPSEEK_API_KEY}`
        },
        proxy: false
      }
    );

    const rawContent = response.data.choices[0].message.content;
    const jsonMatch = rawContent.match(/```json\n([\s\S]*?)\n```/);
    const jsonString = jsonMatch ? jsonMatch[1] : rawContent;
    
    let travelPlan = JSON.parse(jsonString);

    // Enrich with map data
    if (travelPlan.travel_plan && travelPlan.travel_plan.itinerary) {
      for (const dayKey in travelPlan.travel_plan.itinerary) {
        const day = travelPlan.travel_plan.itinerary[dayKey];
        const timeSlots = ['morning', 'lunch', 'afternoon', 'evening'];

        for (const slot of timeSlots) {
          if (day[slot] && day[slot].location) {
            const locationDetails = await getLocationDetails(day[slot].location);
            if (locationDetails) {
              day[slot].locationDetails = {
                name: locationDetails.name,
                address: locationDetails.address,
                coordinates: locationDetails.location,
                rating: locationDetails.biz_ext ? locationDetails.biz_ext.rating : 'N/A',
                photos: locationDetails.photos && locationDetails.photos.length > 0 ? locationDetails.photos.map(p => p.url) : []
              };
            }
          }
        }
      }
    }

    res.json(travelPlan);
  } catch (error) {
    console.error('Travel plan generation error:', error);
    res.status(500).json({
      error: 'Failed to generate travel plan',
      message: 'An error occurred while generating your travel plan'
    });
  }
});

// Save travel plan
router.post('/save', authenticateToken, async (req, res) => {
  try {
    const planData = req.body;
    const userId = req.user.id;
    console.log('Received travel plan data:', req.body);
    if (!planData || !planData.destination) {
      return res.status(400).json({
        error: 'Invalid plan data',
        message: 'Travel plan data is required'
      });
    }

    const {
      destination,
      start_date,
      duration,
      travelers,
      total_budget,
      preferences = [],
      additional_requirements = ''
    } = planData;

    db.run(
      `INSERT INTO travel_plans
       (user_id, destination, start_date, days, travelers, budget, preferences, additional_requirements, plan_data)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        userId,
        destination,
        start_date,
        duration,
        travelers,
        total_budget,
        JSON.stringify(preferences),
        additional_requirements,
        JSON.stringify(planData)
      ],
      function(err) {
        if (err) {
          console.error('Error saving travel plan:', err);
          return res.status(500).json({
            error: 'Failed to save travel plan',
            message: 'Could not save your travel plan'
          });
        }

        res.json({
          success: true,
          planId: this.lastID,
          message: 'Travel plan saved successfully'
        });
      }
    );
  } catch (error) {
    console.error('Save travel plan error:', error);
    res.status(500).json({
      error: 'Failed to save travel plan',
      message: 'An error occurred while saving your travel plan'
    });
  }
});

// Get user's travel plan history
router.get('/history', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id;

    db.all(
      `SELECT id, destination, start_date, days, travelers, budget, created_at
       FROM travel_plans
       WHERE user_id = ?
       ORDER BY created_at DESC`,
      [userId],
      (err, plans) => {
        if (err) {
          console.error('Error fetching travel history:', err);
          return res.status(500).json({
            error: 'Failed to fetch travel history',
            message: 'Could not retrieve your travel plans'
          });
        }

        res.json(plans.map(plan => ({
          id: plan.id,
          destination: plan.destination,
          startDate: plan.start_date,
          days: plan.days,
          travelers: plan.travelers,
          budget: plan.budget,
          createdAt: plan.created_at
        })));
      }
    );
  } catch (error) {
    console.error('Travel history error:', error);
    res.status(500).json({
      error: 'Failed to fetch travel history',
      message: 'An error occurred while retrieving your travel plans'
    });
  }
});

// Get specific travel plan details
router.get('/:id', authenticateToken, (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user.id;

    db.get(
      `SELECT plan_data FROM travel_plans WHERE id = ? AND user_id = ?`,
      [id, userId],
      (err, plan) => {
        if (err) {
          console.error('Error fetching travel plan:', err);
          return res.status(500).json({
            error: 'Failed to fetch travel plan',
            message: 'Could not retrieve the travel plan'
          });
        }

        if (!plan) {
          return res.status(404).json({
            error: 'Travel plan not found',
            message: 'The requested travel plan does not exist'
          });
        }

        res.json(JSON.parse(plan.plan_data));
      }
    );
  } catch (error) {
    console.error('Get travel plan error:', error);
    res.status(500).json({
      error: 'Failed to fetch travel plan',
      message: 'An error occurred while retrieving the travel plan'
    });
  }
});

// Delete travel plan
router.delete('/:id', authenticateToken, (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user.id;

    db.run(
      `DELETE FROM travel_plans WHERE id = ? AND user_id = ?`,
      [id, userId],
      function(err) {
        if (err) {
          console.error('Error deleting travel plan:', err);
          return res.status(500).json({
            error: 'Failed to delete travel plan',
            message: 'Could not delete the travel plan'
          });
        }

        if (this.changes === 0) {
          return res.status(404).json({
            error: 'Travel plan not found',
            message: 'The travel plan does not exist or you do not have permission to delete it'
          });
        }

        res.json({
          success: true,
          message: 'Travel plan deleted successfully'
        });
      }
    );
  } catch (error) {
    console.error('Delete travel plan error:', error);
    res.status(500).json({
      error: 'Failed to delete travel plan',
      message: 'An error occurred while deleting the travel plan'
    });
  }
});

module.exports = router;