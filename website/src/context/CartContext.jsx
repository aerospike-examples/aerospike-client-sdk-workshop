import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { apiUrl } from '../config/api';

const CartContext = createContext();

const cartReducer = (state, action) => {
    switch (action.type) {
        case 'ADD_ITEM':
            const existingItem = state.items.find(item => item.productId === action.payload.productId);
            if (existingItem) {
                return {
                    ...state,
                    items: state.items.map(item =>
                        item.productId === action.payload.productId
                            ? { ...item, quantity: item.quantity + action.payload.quantity }
                            : item
                    )
                };
            } else {
                return {
                    ...state,
                    items: [...state.items, action.payload]
                };
            }
        
        case 'REMOVE_ITEM':
            return {
                ...state,
                items: state.items.filter(item => item.productId !== action.payload.productId)
            };
        
        case 'UPDATE_QUANTITY':
            return {
                ...state,
                items: state.items.map(item =>
                    item.productId === action.payload.productId
                        ? { ...item, quantity: action.payload.quantity }
                        : item
                ).filter(item => item.quantity > 0)
            };
        
        case 'CLEAR_CART':
            return {
                ...state,
                items: []
            };
        
        case 'LOAD_CART':
            return {
                ...state,
                items: action.payload.items || []
            };
        
        default:
            return state;
    }
};

const initialState = {
    items: []
};

// Simple user ID generation for demo purposes
const getUserId = () => {
    let userId = localStorage.getItem('userId');
    if (!userId) {
        userId = 'user_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('userId', userId);
    }
    return userId;
};

export const CartProvider = ({ children }) => {
    const [state, dispatch] = useReducer(cartReducer, initialState);

    // Load cart from backend or localStorage on mount
    useEffect(() => {
        const loadCart = async () => {
            try {
                // Try to load from backend first
                const userId = getUserId();
                const response = await fetch(apiUrl(`cart/${userId}`));
                
                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.items) {
                        dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                        return;
                    }
                }
            } catch (error) {
                console.log('Backend cart not available, using localStorage');
            }

            // Fallback to localStorage
            const savedCart = localStorage.getItem('cart');
            if (savedCart) {
                try {
                    const cartData = JSON.parse(savedCart);
                    // Handle both old format (direct items array) and new format (state object)
                    const items = Array.isArray(cartData) ? cartData : (cartData.items || []);
                    dispatch({ type: 'LOAD_CART', payload: { items } });
                } catch (error) {
                    console.error('Error loading cart from localStorage:', error);
                }
            }
        };

        loadCart();
    }, []);

    // Save cart to localStorage whenever it changes
    useEffect(() => {
        localStorage.setItem('cart', JSON.stringify(state));
    }, [state]);

    const addToCart = async (productId, quantity = 1) => {
        try {
            // Try to use backend API first
            const userId = getUserId();
            const response = await fetch(apiUrl(`cart/${userId}/add`), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    productId: productId,
                    quantity: quantity.toString()
                })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    // Update local state with backend response
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return { success: true };
                }
            }

            // Fallback to local storage approach
            const productResponse = await fetch(apiUrl(`get?prod=${productId}`));
            const productData = await productResponse.json();
            
            if (productData.error) {
                throw new Error(productData.error);
            }

            const product = productData.product;
            const cartItem = {
                productId,
                name: product.name,
                price: product.price,
                image: product.images?.search?.resolutions?.['125X161'] || product.images?.front?.resolutions?.['125X161'],
                brandName: product.brandName,
                quantity
            };

            dispatch({ type: 'ADD_ITEM', payload: cartItem });
            return { success: true };
        } catch (error) {
            console.error('Error adding to cart:', error);
            return { success: false, error: error.message };
        }
    };

    const removeFromCart = async (productId) => {
        try {
            // Try backend API first
            const userId = getUserId();
            const response = await fetch(apiUrl(`cart/${userId}/remove?productId=${productId}`), {
                method: 'DELETE'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend remove failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'REMOVE_ITEM', payload: { productId } });
    };

    const updateQuantity = async (productId, quantity) => {
        try {
            // Try backend API first
            const userId = getUserId();
            const response = await fetch(apiUrl(`cart/${userId}/update`), {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({
                    productId: productId,
                    quantity: quantity.toString()
                })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend update failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'UPDATE_QUANTITY', payload: { productId, quantity } });
    };

    const clearCart = async () => {
        try {
            // Try backend API first
            const userId = getUserId();
            const response = await fetch(apiUrl(`cart/${userId}/clear`), {
                method: 'DELETE'
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    dispatch({ type: 'LOAD_CART', payload: { items: data.items } });
                    return;
                }
            }
        } catch (error) {
            console.log('Backend clear failed, using local fallback');
        }

        // Fallback to local
        dispatch({ type: 'CLEAR_CART' });
    };

    const getCartTotal = () => {
        return state.items.reduce((total, item) => {
            const price = typeof item.price === 'number' ? item.price : 0;
            return total + (price * item.quantity);
        }, 0);
    };

    const getCartItemCount = () => {
        return state.items.reduce((count, item) => count + item.quantity, 0);
    };

    const value = {
        items: state.items,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        getCartTotal,
        getCartItemCount
    };

    return (
        <CartContext.Provider value={value}>
            {children}
        </CartContext.Provider>
    );
};

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};
